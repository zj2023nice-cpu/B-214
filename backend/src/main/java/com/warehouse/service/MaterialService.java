package com.warehouse.service;

import com.warehouse.dto.ImportFailureDetail;
import com.warehouse.dto.ImportResult;
import com.warehouse.entity.Category;
import com.warehouse.entity.Material;
import com.warehouse.entity.Supplier;
import com.warehouse.repository.CategoryRepository;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    private static final int BATCH_SIZE = 500;

    private static final String[] EXCEL_HEADERS = {"物资编号", "物资名称", "规格型号", "单位", "单价", "初始库存", "库存预警值", "分类ID", "供应商ID"};

    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    public Page<Material> searchMaterials(String keyword, Long categoryId, String stockStatus, int page, int size) {
        Specification<Material> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                Predicate codeLike = cb.like(root.get("code"), pattern);
                Predicate nameLike = cb.like(root.get("name"), pattern);
                Predicate specLike = cb.like(root.get("spec"), pattern);
                predicates.add(cb.or(codeLike, nameLike, specLike));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if ("normal".equals(stockStatus)) {
                predicates.add(cb.ge(root.get("stockQuantity"), root.get("alertThreshold")));
            } else if ("warning".equals(stockStatus)) {
                predicates.add(cb.lt(root.get("stockQuantity"), root.get("alertThreshold")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return materialRepository.findAll(spec, pageable);
    }

    public Material getMaterialById(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new RuntimeException("Material not found"));
    }

    public Material saveMaterial(Material material) {
        return materialRepository.save(material);
    }

    public Material updateMaterial(Long id, Material material) {
        Material existing = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("物资不存在"));
        if (material.getCode() != null) existing.setCode(material.getCode());
        if (material.getName() != null) existing.setName(material.getName());
        if (material.getSpec() != null) existing.setSpec(material.getSpec());
        if (material.getUnit() != null) existing.setUnit(material.getUnit());
        if (material.getPrice() != null) existing.setPrice(material.getPrice());
        if (material.getStockQuantity() != null) existing.setStockQuantity(material.getStockQuantity());
        if (material.getAlertThreshold() != null) existing.setAlertThreshold(material.getAlertThreshold());
        if (material.getCategory() != null) existing.setCategory(material.getCategory());
        if (material.getSupplier() != null) existing.setSupplier(material.getSupplier());
        if (material.getImages() != null) existing.setImages(material.getImages());
        return materialRepository.save(existing);
    }

    public void deleteMaterial(Long id) {
        materialRepository.deleteById(id);
    }

    public List<Material> getLowStockMaterials(Integer threshold) {
        return materialRepository.findByStockQuantityLessThan(threshold);
    }

    @Transactional
    public ImportResult importMaterials(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        String lowerName = originalFilename.toLowerCase();
        List<String[]> rows;
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            rows = parseExcel(file.getInputStream());
        } else if (lowerName.endsWith(".csv")) {
            rows = parseCsv(file.getInputStream());
        } else {
            throw new RuntimeException("不支持的文件格式，请上传 Excel(.xlsx) 或 CSV 文件");
        }

        return processImport(rows);
    }

    private List<String[]> parseExcel(InputStream inputStream) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String[] values = new String[EXCEL_HEADERS.length];
                boolean hasData = false;
                for (int j = 0; j < EXCEL_HEADERS.length; j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    values[j] = getCellStringValue(cell).trim();
                    if (!values[j].isEmpty()) hasData = true;
                }
                if (hasData) rows.add(values);
            }
        }
        return rows;
    }

    private String getCellStringValue(Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    yield String.valueOf((long) numVal);
                }
                yield String.valueOf(numVal);
            }
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private List<String[]> parseCsv(InputStream inputStream) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVParser.parse(reader, CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {
            for (CSVRecord record : csvParser) {
                String[] values = new String[EXCEL_HEADERS.length];
                for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                    try {
                        if (i < record.size()) {
                            values[i] = record.get(i);
                        } else {
                            values[i] = "";
                        }
                    } catch (IllegalArgumentException e) {
                        values[i] = "";
                    }
                }
                boolean hasData = Arrays.stream(values).anyMatch(v -> !v.isEmpty());
                if (hasData) rows.add(values);
            }
        }
        return rows;
    }

    private ImportResult processImport(List<String[]> rows) {
        ImportResult result = new ImportResult();
        result.setTotalCount(rows.size());

        if (rows.size() > BATCH_SIZE) {
            throw new RuntimeException("单次导入不能超过 " + BATCH_SIZE + " 条记录，当前: " + rows.size() + " 条");
        }

        Set<String> codesInFile = new HashSet<>();
        List<Material> validMaterials = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowIndex = i + 2;
            String code = row[0];
            String name = row[1];
            String spec = row[2];
            String unit = row[3];
            String priceStr = row[4];
            String stockQuantityStr = row[5];
            String alertThresholdStr = row[6];
            String categoryIdStr = row[7];
            String supplierIdStr = row[8];

            if (code.isEmpty() || name.isEmpty()) {
                result.getFailures().add(new ImportFailureDetail(rowIndex, code, "物资编号和名称为必填项"));
                continue;
            }

            if (materialRepository.existsByCode(code)) {
                result.getFailures().add(new ImportFailureDetail(rowIndex, code, "物资编号已存在"));
                continue;
            }

            if (codesInFile.contains(code)) {
                result.getFailures().add(new ImportFailureDetail(rowIndex, code, "文件中存在重复的物资编号"));
                continue;
            }
            codesInFile.add(code);

            Category category = null;
            if (!categoryIdStr.isEmpty()) {
                try {
                    Long categoryId = Long.parseLong(categoryIdStr);
                    category = categoryRepository.findById(categoryId).orElse(null);
                    if (category == null) {
                        result.getFailures().add(new ImportFailureDetail(rowIndex, code, "分类ID不存在: " + categoryIdStr));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    result.getFailures().add(new ImportFailureDetail(rowIndex, code, "分类ID格式错误: " + categoryIdStr));
                    continue;
                }
            }

            Supplier supplier = null;
            if (!supplierIdStr.isEmpty()) {
                try {
                    Long supplierId = Long.parseLong(supplierIdStr);
                    supplier = supplierRepository.findById(supplierId).orElse(null);
                    if (supplier == null) {
                        result.getFailures().add(new ImportFailureDetail(rowIndex, code, "供应商ID不存在: " + supplierIdStr));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    result.getFailures().add(new ImportFailureDetail(rowIndex, code, "供应商ID格式错误: " + supplierIdStr));
                    continue;
                }
            }

            double price = 0;
            if (!priceStr.isEmpty()) {
                try {
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    result.getFailures().add(new ImportFailureDetail(rowIndex, code, "单价格式错误: " + priceStr));
                    continue;
                }
            }

            int stockQuantity = 0;
            if (!stockQuantityStr.isEmpty()) {
                try {
                    stockQuantity = Integer.parseInt(stockQuantityStr);
                } catch (NumberFormatException e) {
                    result.getFailures().add(new ImportFailureDetail(rowIndex, code, "初始库存格式错误: " + stockQuantityStr));
                    continue;
                }
            }

            int alertThreshold = 10;
            if (!alertThresholdStr.isEmpty()) {
                try {
                    alertThreshold = Integer.parseInt(alertThresholdStr);
                } catch (NumberFormatException e) {
                    result.getFailures().add(new ImportFailureDetail(rowIndex, code, "库存预警值格式错误: " + alertThresholdStr));
                    continue;
                }
            }

            Material material = new Material();
            material.setCode(code);
            material.setName(name);
            material.setSpec(spec);
            material.setUnit(unit);
            material.setPrice(price);
            material.setStockQuantity(stockQuantity);
            material.setAlertThreshold(alertThreshold);
            material.setCategory(category);
            material.setSupplier(supplier);
            validMaterials.add(material);
        }

        if (!validMaterials.isEmpty()) {
            materialRepository.saveAll(validMaterials);
        }

        result.setSuccessCount(validMaterials.size());
        result.setFailureCount(result.getFailures().size());
        return result;
    }

    public byte[] generateImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("物资导入模板");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXCEL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            Row exampleRow = sheet.createRow(1);
            String[] exampleValues = {"MAT001", "螺丝", "M8x30", "个", "0.5", "1000", "100", "1", "1"};
            for (int i = 0; i < exampleValues.length; i++) {
                exampleRow.createCell(i).setCellValue(exampleValues[i]);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // Category Operations
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    // Supplier Operations
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }
}

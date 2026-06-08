package com.warehouse.service;

import com.warehouse.dto.DailyTrendDTO;
import com.warehouse.dto.TurnoverRateDTO;
import com.warehouse.entity.InboundRecord;
import com.warehouse.entity.Material;
import com.warehouse.entity.OutboundRecord;
import com.warehouse.entity.Warehouse;
import com.warehouse.repository.InboundRecordRepository;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.OutboundRecordRepository;
import com.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InboundRecordRepository inboundRecordRepository;
    private final OutboundRecordRepository outboundRecordRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryService warehouseInventoryService;

    @Transactional
    public InboundRecord processInbound(InboundRecord record) {
        record.setInboundTime(LocalDateTime.now());

        Material material = materialRepository.findById(record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material not found"));

        record.setMaterial(material);

        if (record.getWarehouse() != null && record.getWarehouse().getId() != null) {
            Warehouse warehouse = warehouseRepository.findById(record.getWarehouse().getId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            record.setWarehouse(warehouse);

            warehouseInventoryService.addToWarehouse(
                    warehouse.getId(), material.getId(), record.getQuantity());
        } else {
            material.setStockQuantity(material.getStockQuantity() + record.getQuantity());
            materialRepository.save(material);
        }

        return inboundRecordRepository.save(record);
    }

    @Transactional
    public OutboundRecord processOutbound(OutboundRecord record) {
        record.setOutboundTime(LocalDateTime.now());

        Material material = materialRepository.findById(record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material not found"));

        record.setMaterial(material);

        if (record.getWarehouse() != null && record.getWarehouse().getId() != null) {
            Warehouse warehouse = warehouseRepository.findById(record.getWarehouse().getId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            record.setWarehouse(warehouse);

            warehouseInventoryService.deductFromWarehouse(
                    warehouse.getId(), material.getId(), record.getQuantity());
        } else {
            if (material.getStockQuantity() < record.getQuantity()) {
                throw new RuntimeException("Insufficient stock! Current: " + material.getStockQuantity());
            }
            material.setStockQuantity(material.getStockQuantity() - record.getQuantity());
            materialRepository.save(material);
        }

        return outboundRecordRepository.save(record);
    }

    public List<InboundRecord> getAllInboundRecords() {
        return inboundRecordRepository.findAll();
    }

    public List<OutboundRecord> getAllOutboundRecords() {
        return outboundRecordRepository.findAll();
    }

    public List<DailyTrendDTO> getDailyTrend(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        Map<LocalDate, long[]> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            trendMap.put(startDate.plusDays(i), new long[]{0, 0});
        }

        List<Object[]> inboundTotals = inboundRecordRepository.findDailyInboundTotals(startDateTime);
        for (Object[] row : inboundTotals) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long total = ((Number) row[1]).longValue();
            if (trendMap.containsKey(date)) {
                trendMap.get(date)[0] = total;
            }
        }

        List<Object[]> outboundTotals = outboundRecordRepository.findDailyOutboundTotals(startDateTime);
        for (Object[] row : outboundTotals) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long total = ((Number) row[1]).longValue();
            if (trendMap.containsKey(date)) {
                trendMap.get(date)[1] = total;
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return trendMap.entrySet().stream()
                .map(e -> new DailyTrendDTO(
                        e.getKey().format(formatter),
                        e.getValue()[0],
                        e.getValue()[1]))
                .toList();
    }

    public List<TurnoverRateDTO> getTurnoverRates(String month) {
        LocalDate targetDate = (month != null && !month.isBlank())
                ? LocalDate.parse(month + "-01")
                : LocalDate.now();
        LocalDateTime start = targetDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = targetDate.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        List<Material> allMaterials = materialRepository.findAll();

        Map<Long, Long> outboundMap = new HashMap<>();
        for (Object[] row : outboundRecordRepository.findMonthlyOutboundByMaterial(start, end)) {
            Long materialId = ((Number) row[0]).longValue();
            Long total = ((Number) row[1]).longValue();
            outboundMap.put(materialId, total);
        }

        Map<Long, Long> inboundMap = new HashMap<>();
        for (Object[] row : inboundRecordRepository.findMonthlyInboundByMaterial(start, end)) {
            Long materialId = ((Number) row[0]).longValue();
            Long total = ((Number) row[1]).longValue();
            inboundMap.put(materialId, total);
        }

        List<TurnoverRateDTO> result = new ArrayList<>();
        for (Material m : allMaterials) {
            if (m.getStockQuantity() == null || m.getStockQuantity() == 0) continue;

            long outboundTotal = outboundMap.getOrDefault(m.getId(), 0L);
            long inboundTotal = inboundMap.getOrDefault(m.getId(), 0L);

            long endStock = m.getStockQuantity();
            long beginStock = endStock - inboundTotal + outboundTotal;
            if (beginStock < 0) beginStock = 0;

            double avgStock = (beginStock + endStock) / 2.0;
            if (avgStock == 0) continue;

            double turnoverRate = outboundTotal / avgStock;

            result.add(new TurnoverRateDTO(
                    m.getCode(),
                    m.getName(),
                    outboundTotal,
                    Math.round(avgStock * 100.0) / 100.0,
                    Math.round(turnoverRate * 100.0) / 100.0
            ));
        }

        result.sort((a, b) -> Double.compare(b.getTurnoverRate(), a.getTurnoverRate()));
        return result;
    }

    public byte[] exportTurnoverRatesExcel(String month) throws IOException {
        List<TurnoverRateDTO> data = getTurnoverRates(month);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("库存周转率报表");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"物资编号", "物资名称", "出库总量", "平均库存量", "周转率"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            for (int i = 0; i < data.size(); i++) {
                TurnoverRateDTO dto = data.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(dto.getCode());
                row.createCell(1).setCellValue(dto.getName());
                row.createCell(2).setCellValue(dto.getOutboundTotal());
                row.createCell(3).setCellValue(dto.getAvgStock());
                row.createCell(4).setCellValue(dto.getTurnoverRate());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}

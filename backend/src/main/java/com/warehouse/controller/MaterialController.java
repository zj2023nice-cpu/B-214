package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.annotation.RequireRole;
import com.warehouse.common.ApiResponse;
import com.warehouse.dto.ImportResult;
import com.warehouse.entity.Material;
import com.warehouse.entity.OperationType;
import com.warehouse.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MaterialController {
    private final MaterialService materialService;

    @GetMapping
    public ApiResponse<List<Material>> getAllMaterials() {
        return ApiResponse.success(materialService.getAllMaterials());
    }

    @PostMapping
    @OperationLog(type = OperationType.CREATE, description = "新增物资", target = "物资")
    public ApiResponse<Material> createMaterial(@RequestBody Material material) {
        return ApiResponse.success(materialService.saveMaterial(material));
    }

    @PutMapping("/{id}")
    @OperationLog(type = OperationType.UPDATE, description = "修改物资", target = "物资")
    public ApiResponse<Material> updateMaterial(@PathVariable Long id, @RequestBody Material material) {
        return ApiResponse.success(materialService.updateMaterial(id, material));
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    @OperationLog(type = OperationType.DELETE, description = "删除物资", target = "物资")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<Material>> getLowStock(@RequestParam(defaultValue = "10") Integer threshold) {
        return ApiResponse.success(materialService.getLowStockMaterials(threshold));
    }

    @PostMapping("/import")
    @OperationLog(type = OperationType.IMPORT, description = "导入物资", target = "物资")
    public ApiResponse<ImportResult> importMaterials(@RequestParam("file") MultipartFile file) {
        try {
            ImportResult result = materialService.importMaterials(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] template = materialService.generateImportTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=material_import_template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(template);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

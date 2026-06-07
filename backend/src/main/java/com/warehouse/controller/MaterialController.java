package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
import com.warehouse.entity.Category;
import com.warehouse.entity.Material;
import com.warehouse.entity.Supplier;
import com.warehouse.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<Material> createMaterial(@RequestBody Material material) {
        return ApiResponse.success(materialService.saveMaterial(material));
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ApiResponse.success(null);
    }
    
    @GetMapping("/low-stock")
    public ApiResponse<List<Material>> getLowStock(@RequestParam(defaultValue = "10") Integer threshold) {
        return ApiResponse.success(materialService.getLowStockMaterials(threshold));
    }
}

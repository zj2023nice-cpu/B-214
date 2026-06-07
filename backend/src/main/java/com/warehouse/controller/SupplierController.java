package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
import com.warehouse.entity.Supplier;
import com.warehouse.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierController {
    private final SupplierService supplierService;

    @GetMapping
    public ApiResponse<List<Supplier>> getAllSuppliers() {
        return ApiResponse.success(supplierService.getAllSuppliers());
    }

    @PostMapping
    public ApiResponse<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return ApiResponse.success(supplierService.createSupplier(supplier));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ApiResponse.success(null);
    }
}

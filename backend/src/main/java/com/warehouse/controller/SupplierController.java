package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.common.ApiResponse;
import com.warehouse.entity.OperationType;
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
    @OperationLog(type = OperationType.CREATE, description = "新增供应商", target = "供应商")
    public ApiResponse<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return ApiResponse.success(supplierService.createSupplier(supplier));
    }

    @DeleteMapping("/{id}")
    @OperationLog(type = OperationType.DELETE, description = "删除供应商", target = "供应商")
    public ApiResponse<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ApiResponse.success(null);
    }
}

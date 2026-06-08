package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.annotation.RequireRole;
import com.warehouse.common.ApiResponse;
import com.warehouse.dto.BatchDeleteResult;
import com.warehouse.entity.OperationType;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.Warehouse;
import com.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WarehouseController {
    private final WarehouseService warehouseService;

    @GetMapping
    public ApiResponse<List<Warehouse>> getAll() {
        return ApiResponse.success(warehouseService.getAllWarehouses());
    }

    @PostMapping
    @OperationLog(type = OperationType.CREATE, description = "新增仓库", target = "仓库")
    public ApiResponse<Warehouse> create(@RequestBody Warehouse warehouse) {
        return ApiResponse.success(warehouseService.saveWarehouse(warehouse));
    }

    @PutMapping("/{id}")
    @OperationLog(type = OperationType.UPDATE, description = "修改仓库", target = "仓库")
    public ApiResponse<Warehouse> update(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        return ApiResponse.success(warehouseService.updateWarehouse(id, warehouse));
    }
    
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    @OperationLog(type = OperationType.DELETE, description = "删除仓库", target = "仓库")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/batch")
    @RequireRole("ADMIN")
    @OperationLog(type = OperationType.DELETE, description = "批量删除仓库", target = "仓库")
    public ApiResponse<BatchDeleteResult> batchDelete(@RequestBody List<Long> ids) {
        return ApiResponse.success(warehouseService.batchDeleteWarehouses(ids));
    }
    
    // Shelf Endpoints
    @GetMapping("/{id}/shelves")
    public ApiResponse<List<Shelf>> getShelves(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getShelvesByWarehouse(id));
    }
    
    @PostMapping("/shelves")
    @OperationLog(type = OperationType.CREATE, description = "新增货架", target = "货架")
    public ApiResponse<Shelf> createShelf(@RequestBody Shelf shelf) {
        return ApiResponse.success(warehouseService.saveShelf(shelf));
    }
    
    @DeleteMapping("/shelves/{id}")
    @OperationLog(type = OperationType.DELETE, description = "删除货架", target = "货架")
    public ApiResponse<Void> deleteShelf(@PathVariable Long id) {
        warehouseService.deleteShelf(id);
        return ApiResponse.success(null);
    }
}

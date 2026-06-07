package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
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
    public ApiResponse<Warehouse> create(@RequestBody Warehouse warehouse) {
        return ApiResponse.success(warehouseService.saveWarehouse(warehouse));
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ApiResponse.success(null);
    }
    
    // Shelf Endpoints
    @GetMapping("/{id}/shelves")
    public ApiResponse<List<Shelf>> getShelves(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getShelvesByWarehouse(id));
    }
    
    @PostMapping("/shelves")
    public ApiResponse<Shelf> createShelf(@RequestBody Shelf shelf) {
        return ApiResponse.success(warehouseService.saveShelf(shelf));
    }
    
    @DeleteMapping("/shelves/{id}")
    public ApiResponse<Void> deleteShelf(@PathVariable Long id) {
        warehouseService.deleteShelf(id);
        return ApiResponse.success(null);
    }
}

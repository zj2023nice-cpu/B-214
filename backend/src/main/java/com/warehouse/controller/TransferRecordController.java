package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
import com.warehouse.entity.TransferRecord;
import com.warehouse.entity.WarehouseInventory;
import com.warehouse.service.TransferRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransferRecordController {
    private final TransferRecordService transferRecordService;

    @PostMapping
    public ApiResponse<TransferRecord> createTransfer(@RequestBody TransferRecord record) {
        return ApiResponse.success(transferRecordService.createTransfer(record));
    }

    @GetMapping
    public ApiResponse<List<TransferRecord>> getAllTransfers() {
        return ApiResponse.success(transferRecordService.getAllTransfers());
    }

    @GetMapping("/{id}")
    public ApiResponse<TransferRecord> getTransferById(@PathVariable Long id) {
        return ApiResponse.success(transferRecordService.getTransferById(id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<TransferRecord> cancelTransfer(@PathVariable Long id) {
        return ApiResponse.success(transferRecordService.cancelTransfer(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTransfer(@PathVariable Long id) {
        transferRecordService.deleteTransfer(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/warehouse-inventory/{warehouseId}")
    public ApiResponse<List<WarehouseInventory>> getWarehouseInventory(@PathVariable Long warehouseId) {
        return ApiResponse.success(transferRecordService.getWarehouseInventory(warehouseId));
    }
}

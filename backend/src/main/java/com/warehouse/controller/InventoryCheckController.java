package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.annotation.RequireRole;
import com.warehouse.common.ApiResponse;
import com.warehouse.entity.InventoryCheck;
import com.warehouse.entity.InventoryCheckDetail;
import com.warehouse.entity.OperationType;
import com.warehouse.service.InventoryCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory-checks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryCheckController {
    private final InventoryCheckService inventoryCheckService;

    @PostMapping
    @RequireRole("ADMIN")
    @OperationLog(type = OperationType.CHECK, description = "创建盘点", target = "盘点")
    public ApiResponse<InventoryCheck> createCheck(@RequestBody Map<String, String> body) {
        String remark = body.getOrDefault("remark", "");
        return ApiResponse.success(inventoryCheckService.createCheck(remark));
    }

    @PostMapping("/{id}/submit")
    @OperationLog(type = OperationType.UPDATE, description = "提交盘点结果", target = "盘点")
    public ApiResponse<InventoryCheck> submitCheckResult(
            @PathVariable Long id,
            @RequestBody List<InventoryCheckDetail> details) {
        return ApiResponse.success(inventoryCheckService.submitCheckResult(id, details));
    }

    @GetMapping
    public ApiResponse<List<InventoryCheck>> getAllChecks() {
        return ApiResponse.success(inventoryCheckService.getAllChecks());
    }

    @GetMapping("/{id}")
    public ApiResponse<InventoryCheck> getCheckById(@PathVariable Long id) {
        return ApiResponse.success(inventoryCheckService.getCheckById(id));
    }
}

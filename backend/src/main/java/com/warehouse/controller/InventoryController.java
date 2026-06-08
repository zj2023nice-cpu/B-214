package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.common.ApiResponse;
import com.warehouse.dto.DailyTrendDTO;
import com.warehouse.entity.InboundRecord;
import com.warehouse.entity.OperationType;
import com.warehouse.entity.OutboundRecord;
import com.warehouse.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping("/inbound")
    @OperationLog(type = OperationType.INBOUND, description = "物资入库", target = "库存")
    public ApiResponse<InboundRecord> inbound(@RequestBody InboundRecord record) {
        return ApiResponse.success(inventoryService.processInbound(record));
    }

    @PostMapping("/outbound")
    @OperationLog(type = OperationType.OUTBOUND, description = "物资出库", target = "库存")
    public ApiResponse<OutboundRecord> outbound(@RequestBody OutboundRecord record) {
        return ApiResponse.success(inventoryService.processOutbound(record));
    }

    @GetMapping("/records/inbound")
    public ApiResponse<List<InboundRecord>> getInboundRecords() {
        return ApiResponse.success(inventoryService.getAllInboundRecords());
    }

    @GetMapping("/records/outbound")
    public ApiResponse<List<OutboundRecord>> getOutboundRecords() {
        return ApiResponse.success(inventoryService.getAllOutboundRecords());
    }

    @GetMapping("/statistics/trend")
    public ApiResponse<List<DailyTrendDTO>> getTrend(@RequestParam(defaultValue = "7") int days) {
        if (days < 1) days = 1;
        if (days > 30) days = 30;
        return ApiResponse.success(inventoryService.getDailyTrend(days));
    }
}

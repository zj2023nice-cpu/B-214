package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
import com.warehouse.entity.InboundRecord;
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
    public ApiResponse<InboundRecord> inbound(@RequestBody InboundRecord record) {
        return ApiResponse.success(inventoryService.processInbound(record));
    }

    @PostMapping("/outbound")
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
}

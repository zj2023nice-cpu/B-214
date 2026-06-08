package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.annotation.RequireRole;
import com.warehouse.common.ApiResponse;
import com.warehouse.dto.DailyTrendDTO;
import com.warehouse.dto.DashboardStatsDTO;
import com.warehouse.dto.TurnoverRateDTO;
import com.warehouse.entity.InboundRecord;
import com.warehouse.entity.OperationType;
import com.warehouse.entity.OutboundRecord;
import com.warehouse.entity.OutboundStatus;
import com.warehouse.repository.OutboundRecordRepository;
import com.warehouse.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryController {
    private final InventoryService inventoryService;
    private final OutboundRecordRepository outboundRecordRepository;

    @PostMapping("/outbound/approve/{id}")
    @RequireRole("ADMIN")
    @OperationLog(type = OperationType.OUTBOUND, description = "审批通过出库申请", target = "库存")
    public ApiResponse<OutboundRecord> approveOutbound(@PathVariable Long id) {
        return ApiResponse.success(inventoryService.approveOutbound(id));
    }

    @PostMapping("/outbound/reject/{id}")
    @RequireRole("ADMIN")
    @OperationLog(type = OperationType.OUTBOUND, description = "拒绝出库申请", target = "库存")
    public ApiResponse<OutboundRecord> rejectOutbound(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String rejectReason = body.get("rejectReason");
        if (rejectReason == null || rejectReason.isBlank()) {
            return ApiResponse.error(400, "拒绝原因不能为空");
        }
        return ApiResponse.success(inventoryService.rejectOutbound(id, rejectReason));
    }

    @GetMapping("/outbound/pending")
    @RequireRole("ADMIN")
    public ApiResponse<List<OutboundRecord>> getPendingOutboundRecords() {
        return ApiResponse.success(outboundRecordRepository.findByStatusOrderByOutboundTimeDesc(OutboundStatus.PENDING));
    }

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
    public ApiResponse<List<InboundRecord>> getInboundRecords(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            return ApiResponse.success(inventoryService.getInboundRecordsByDateRange(startDateTime, endDateTime));
        } else if (startDate != null) {
            return ApiResponse.success(inventoryService.getInboundRecordsAfterDate(startDate.atStartOfDay()));
        } else if (endDate != null) {
            return ApiResponse.success(inventoryService.getInboundRecordsBeforeDate(endDate.atTime(23, 59, 59)));
        }
        return ApiResponse.success(inventoryService.getAllInboundRecords());
    }

    @GetMapping("/records/outbound")
    public ApiResponse<List<OutboundRecord>> getOutboundRecords(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            return ApiResponse.success(inventoryService.getOutboundRecordsByDateRange(startDateTime, endDateTime));
        } else if (startDate != null) {
            return ApiResponse.success(inventoryService.getOutboundRecordsAfterDate(startDate.atStartOfDay()));
        } else if (endDate != null) {
            return ApiResponse.success(inventoryService.getOutboundRecordsBeforeDate(endDate.atTime(23, 59, 59)));
        }
        return ApiResponse.success(inventoryService.getAllOutboundRecords());
    }

    @GetMapping("/records/export")
    public ResponseEntity<byte[]> exportRecords(
            @RequestParam String type,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;
            if (startDate != null) {
                startDateTime = startDate.atStartOfDay();
            }
            if (endDate != null) {
                endDateTime = endDate.atTime(23, 59, 59);
            }

            byte[] data = inventoryService.exportRecordsCsv(type, startDateTime, endDateTime);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateRange;
            if (startDate != null && endDate != null) {
                dateRange = startDate.format(fmt) + "_" + endDate.format(fmt);
            } else if (startDate != null) {
                dateRange = startDate.format(fmt) + "_latest";
            } else if (endDate != null) {
                dateRange = "earliest_" + endDate.format(fmt);
            } else {
                dateRange = "all";
            }
            String filename = type.equals("inbound")
                    ? "inbound_records_" + dateRange + ".csv"
                    : "outbound_records_" + dateRange + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics/trend")
    public ApiResponse<List<DailyTrendDTO>> getTrend(@RequestParam(defaultValue = "7") int days) {
        if (days < 1) days = 1;
        if (days > 30) days = 30;
        return ApiResponse.success(inventoryService.getDailyTrend(days));
    }

    @GetMapping("/statistics/dashboard")
    public ApiResponse<DashboardStatsDTO> getDashboardStats() {
        return ApiResponse.success(inventoryService.getDashboardStats());
    }

    @GetMapping("/turnover-rate")
    public ApiResponse<List<TurnoverRateDTO>> getTurnoverRate(@RequestParam(required = false) String month) {
        return ApiResponse.success(inventoryService.getTurnoverRates(month));
    }

    @GetMapping("/turnover-rate/export")
    public ResponseEntity<byte[]> exportTurnoverRate(@RequestParam(required = false) String month) {
        try {
            byte[] data = inventoryService.exportTurnoverRatesExcel(month);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=turnover_rate_report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

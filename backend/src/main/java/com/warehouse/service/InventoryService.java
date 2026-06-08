package com.warehouse.service;

import com.warehouse.dto.DailyTrendDTO;
import com.warehouse.entity.InboundRecord;
import com.warehouse.entity.Material;
import com.warehouse.entity.OutboundRecord;
import com.warehouse.entity.Warehouse;
import com.warehouse.entity.WarehouseInventory;
import com.warehouse.repository.InboundRecordRepository;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.OutboundRecordRepository;
import com.warehouse.repository.WarehouseInventoryRepository;
import com.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InboundRecordRepository inboundRecordRepository;
    private final OutboundRecordRepository outboundRecordRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;

    @Transactional
    public InboundRecord processInbound(InboundRecord record) {
        record.setInboundTime(LocalDateTime.now());

        Material material = materialRepository.findById(record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material not found"));

        material.setStockQuantity(material.getStockQuantity() + record.getQuantity());
        materialRepository.save(material);

        if (record.getWarehouse() != null && record.getWarehouse().getId() != null) {
            Warehouse warehouse = warehouseRepository.findById(record.getWarehouse().getId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            record.setWarehouse(warehouse);

            WarehouseInventory wi = warehouseInventoryRepository
                    .findByWarehouseIdAndMaterialId(warehouse.getId(), material.getId())
                    .orElseGet(() -> {
                        WarehouseInventory newWi = new WarehouseInventory();
                        newWi.setWarehouse(warehouse);
                        newWi.setMaterial(material);
                        newWi.setQuantity(0);
                        return newWi;
                    });
            wi.setQuantity(wi.getQuantity() + record.getQuantity());
            warehouseInventoryRepository.save(wi);
        }

        record.setMaterial(material);
        return inboundRecordRepository.save(record);
    }

    @Transactional
    public OutboundRecord processOutbound(OutboundRecord record) {
        record.setOutboundTime(LocalDateTime.now());

        Material material = materialRepository.findById(record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material not found"));

        if (material.getStockQuantity() < record.getQuantity()) {
            throw new RuntimeException("Insufficient stock! Current: " + material.getStockQuantity());
        }

        material.setStockQuantity(material.getStockQuantity() - record.getQuantity());
        materialRepository.save(material);

        if (record.getWarehouse() != null && record.getWarehouse().getId() != null) {
            Warehouse warehouse = warehouseRepository.findById(record.getWarehouse().getId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            record.setWarehouse(warehouse);

            WarehouseInventory wi = warehouseInventoryRepository
                    .findByWarehouseIdAndMaterialId(warehouse.getId(), material.getId())
                    .orElseThrow(() -> new RuntimeException("该仓库中无此物资库存记录"));

            if (wi.getQuantity() < record.getQuantity()) {
                throw new RuntimeException("仓库库存不足！当前库存: " + wi.getQuantity());
            }

            wi.setQuantity(wi.getQuantity() - record.getQuantity());
            warehouseInventoryRepository.save(wi);
        }

        record.setMaterial(material);
        return outboundRecordRepository.save(record);
    }

    public List<InboundRecord> getAllInboundRecords() {
        return inboundRecordRepository.findAll();
    }

    public List<OutboundRecord> getAllOutboundRecords() {
        return outboundRecordRepository.findAll();
    }

    public List<DailyTrendDTO> getDailyTrend(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        Map<LocalDate, long[]> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            trendMap.put(startDate.plusDays(i), new long[]{0, 0});
        }

        List<Object[]> inboundTotals = inboundRecordRepository.findDailyInboundTotals(startDateTime);
        for (Object[] row : inboundTotals) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long total = ((Number) row[1]).longValue();
            if (trendMap.containsKey(date)) {
                trendMap.get(date)[0] = total;
            }
        }

        List<Object[]> outboundTotals = outboundRecordRepository.findDailyOutboundTotals(startDateTime);
        for (Object[] row : outboundTotals) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long total = ((Number) row[1]).longValue();
            if (trendMap.containsKey(date)) {
                trendMap.get(date)[1] = total;
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return trendMap.entrySet().stream()
                .map(e -> new DailyTrendDTO(
                        e.getKey().format(formatter),
                        e.getValue()[0],
                        e.getValue()[1]))
                .toList();
    }
}

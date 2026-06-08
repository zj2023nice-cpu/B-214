package com.warehouse.service;

import com.warehouse.entity.Material;
import com.warehouse.entity.TransferRecord;
import com.warehouse.entity.TransferRecord.TransferStatus;
import com.warehouse.entity.Warehouse;
import com.warehouse.entity.WarehouseInventory;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.TransferRecordRepository;
import com.warehouse.repository.WarehouseInventoryRepository;
import com.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferRecordService {
    private final TransferRecordRepository transferRecordRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;

    @Transactional
    public TransferRecord createTransfer(TransferRecord record) {
        Long sourceId = record.getSourceWarehouse().getId();
        Long targetId = record.getTargetWarehouse().getId();

        if (sourceId.equals(targetId)) {
            throw new RuntimeException("源仓库和目标仓库不能相同");
        }

        Warehouse sourceWarehouse = warehouseRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("源仓库不存在"));
        Warehouse targetWarehouse = warehouseRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("目标仓库不存在"));

        Material material = materialRepository.findById(record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("物资不存在"));

        if (material.getStockQuantity() < record.getQuantity()) {
            throw new RuntimeException("总库存不足！当前总库存: " + material.getStockQuantity());
        }

        WarehouseInventory sourceInventory = warehouseInventoryRepository
                .findByWarehouseIdAndMaterialId(sourceId, material.getId())
                .orElseThrow(() -> new RuntimeException("源仓库中无此物资库存记录"));

        if (sourceInventory.getQuantity() < record.getQuantity()) {
            throw new RuntimeException("源仓库库存不足！当前库存: " + sourceInventory.getQuantity());
        }

        sourceInventory.setQuantity(sourceInventory.getQuantity() - record.getQuantity());
        warehouseInventoryRepository.save(sourceInventory);

        WarehouseInventory targetInventory = warehouseInventoryRepository
                .findByWarehouseIdAndMaterialId(targetId, material.getId())
                .orElseGet(() -> {
                    WarehouseInventory newWi = new WarehouseInventory();
                    newWi.setWarehouse(targetWarehouse);
                    newWi.setMaterial(material);
                    newWi.setQuantity(0);
                    return newWi;
                });
        targetInventory.setQuantity(targetInventory.getQuantity() + record.getQuantity());
        warehouseInventoryRepository.save(targetInventory);

        record.setSerialNo(generateSerialNo());
        record.setTransferTime(LocalDateTime.now());
        record.setSourceWarehouse(sourceWarehouse);
        record.setTargetWarehouse(targetWarehouse);
        record.setMaterial(material);
        record.setStatus(TransferStatus.COMPLETED);

        return transferRecordRepository.save(record);
    }

    public List<TransferRecord> getAllTransfers() {
        return transferRecordRepository.findAll();
    }

    public TransferRecord getTransferById(Long id) {
        return transferRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("调拨记录不存在"));
    }

    @Transactional
    public TransferRecord cancelTransfer(Long id) {
        TransferRecord record = transferRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("调拨记录不存在"));

        if (record.getStatus() == TransferStatus.CANCELLED) {
            throw new RuntimeException("调拨记录已取消");
        }

        WarehouseInventory targetInventory = warehouseInventoryRepository
                .findByWarehouseIdAndMaterialId(record.getTargetWarehouse().getId(), record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("目标仓库中无此物资库存记录"));

        if (targetInventory.getQuantity() < record.getQuantity()) {
            throw new RuntimeException("目标仓库库存不足，无法回退！当前库存: " + targetInventory.getQuantity());
        }

        targetInventory.setQuantity(targetInventory.getQuantity() - record.getQuantity());
        warehouseInventoryRepository.save(targetInventory);

        WarehouseInventory sourceInventory = warehouseInventoryRepository
                .findByWarehouseIdAndMaterialId(record.getSourceWarehouse().getId(), record.getMaterial().getId())
                .orElseGet(() -> {
                    WarehouseInventory newWi = new WarehouseInventory();
                    newWi.setWarehouse(record.getSourceWarehouse());
                    newWi.setMaterial(record.getMaterial());
                    newWi.setQuantity(0);
                    return newWi;
                });
        sourceInventory.setQuantity(sourceInventory.getQuantity() + record.getQuantity());
        warehouseInventoryRepository.save(sourceInventory);

        record.setStatus(TransferStatus.CANCELLED);
        return transferRecordRepository.save(record);
    }

    @Transactional
    public void deleteTransfer(Long id) {
        TransferRecord record = transferRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("调拨记录不存在"));

        if (record.getStatus() == TransferStatus.COMPLETED) {
            WarehouseInventory targetInventory = warehouseInventoryRepository
                    .findByWarehouseIdAndMaterialId(record.getTargetWarehouse().getId(), record.getMaterial().getId())
                    .orElseThrow(() -> new RuntimeException("目标仓库中无此物资库存记录"));

            if (targetInventory.getQuantity() < record.getQuantity()) {
                throw new RuntimeException("目标仓库库存不足，无法回退！当前库存: " + targetInventory.getQuantity());
            }

            targetInventory.setQuantity(targetInventory.getQuantity() - record.getQuantity());
            warehouseInventoryRepository.save(targetInventory);

            WarehouseInventory sourceInventory = warehouseInventoryRepository
                    .findByWarehouseIdAndMaterialId(record.getSourceWarehouse().getId(), record.getMaterial().getId())
                    .orElseGet(() -> {
                        WarehouseInventory newWi = new WarehouseInventory();
                        newWi.setWarehouse(record.getSourceWarehouse());
                        newWi.setMaterial(record.getMaterial());
                        newWi.setQuantity(0);
                        return newWi;
                    });
            sourceInventory.setQuantity(sourceInventory.getQuantity() + record.getQuantity());
            warehouseInventoryRepository.save(sourceInventory);
        }

        transferRecordRepository.delete(record);
    }

    public List<WarehouseInventory> getWarehouseInventory(Long warehouseId) {
        return warehouseInventoryRepository.findByWarehouseId(warehouseId);
    }

    private String generateSerialNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "TR-" + LocalDateTime.now().format(formatter);
    }
}

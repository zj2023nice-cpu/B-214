package com.warehouse.service;

import com.warehouse.entity.Material;
import com.warehouse.entity.Warehouse;
import com.warehouse.entity.WarehouseInventory;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.WarehouseInventoryRepository;
import com.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseInventoryService {
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;

    public WarehouseInventory addToWarehouse(Long warehouseId, Long materialId, int quantity) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("仓库不存在"));
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("物资不存在"));

        WarehouseInventory wi = warehouseInventoryRepository
                .findByWarehouseIdAndMaterialId(warehouseId, materialId)
                .orElseGet(() -> {
                    WarehouseInventory newWi = new WarehouseInventory();
                    newWi.setWarehouse(warehouse);
                    newWi.setMaterial(material);
                    newWi.setQuantity(0);
                    return newWi;
                });

        wi.setQuantity(wi.getQuantity() + quantity);
        warehouseInventoryRepository.save(wi);
        syncMaterialStockQuantity(materialId);
        return wi;
    }

    public WarehouseInventory deductFromWarehouse(Long warehouseId, Long materialId, int quantity) {
        WarehouseInventory wi = warehouseInventoryRepository
                .findByWarehouseIdAndMaterialId(warehouseId, materialId)
                .orElseThrow(() -> new RuntimeException("该仓库中无此物资库存记录"));

        if (wi.getQuantity() < quantity) {
            throw new RuntimeException("仓库库存不足！当前库存: " + wi.getQuantity());
        }

        wi.setQuantity(wi.getQuantity() - quantity);
        warehouseInventoryRepository.save(wi);
        syncMaterialStockQuantity(materialId);
        return wi;
    }

    public void syncMaterialStockQuantity(Long materialId) {
        List<WarehouseInventory> allInventory = warehouseInventoryRepository.findByMaterialId(materialId);
        int total = allInventory.stream().mapToInt(WarehouseInventory::getQuantity).sum();

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("物资不存在"));
        material.setStockQuantity(total);
        materialRepository.save(material);
    }

    public List<WarehouseInventory> getWarehouseInventory(Long warehouseId) {
        return warehouseInventoryRepository.findByWarehouseId(warehouseId);
    }
}

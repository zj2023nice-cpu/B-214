package com.warehouse.repository;

import com.warehouse.entity.WarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, Long> {
    Optional<WarehouseInventory> findByWarehouseIdAndMaterialId(Long warehouseId, Long materialId);

    List<WarehouseInventory> findByWarehouseId(Long warehouseId);

    List<WarehouseInventory> findByMaterialId(Long materialId);

    boolean existsByWarehouseId(Long warehouseId);

    boolean existsByMaterialId(Long materialId);
}

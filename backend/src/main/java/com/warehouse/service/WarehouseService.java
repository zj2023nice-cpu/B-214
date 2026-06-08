package com.warehouse.service;

import com.warehouse.dto.BatchDeleteFailureDetail;
import com.warehouse.dto.BatchDeleteResult;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.Warehouse;
import com.warehouse.repository.ShelfRepository;
import com.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;
    private final ShelfRepository shelfRepository;

    // Warehouse Operations
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    public Warehouse saveWarehouse(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    public Warehouse updateWarehouse(Long id, Warehouse warehouse) {
        Warehouse existing = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓库不存在"));
        if (warehouse.getCode() != null) existing.setCode(warehouse.getCode());
        if (warehouse.getName() != null) existing.setName(warehouse.getName());
        if (warehouse.getAddress() != null) existing.setAddress(warehouse.getAddress());
        if (warehouse.getManager() != null) existing.setManager(warehouse.getManager());
        return warehouseRepository.save(existing);
    }

    public void deleteWarehouse(Long id) {
        warehouseRepository.deleteById(id);
    }

    @Transactional
    public BatchDeleteResult batchDeleteWarehouses(List<Long> ids) {
        BatchDeleteResult result = new BatchDeleteResult();
        result.setTotalCount(ids.size());
        for (Long id : ids) {
            if (!warehouseRepository.existsById(id)) {
                result.getFailures().add(new BatchDeleteFailureDetail(id, "仓库不存在"));
            }
        }
        if (!result.getFailures().isEmpty()) {
            result.setSuccessCount(0);
            result.setFailureCount(result.getFailures().size());
            return result;
        }
        warehouseRepository.deleteAllById(ids);
        result.setSuccessCount(ids.size());
        result.setFailureCount(0);
        return result;
    }
    
    public Warehouse getWarehouseById(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new RuntimeException("Warehouse not found"));
    }

    // Shelf Operations
    public List<Shelf> getShelvesByWarehouse(Long warehouseId) {
        return shelfRepository.findByWarehouseId(warehouseId);
    }

    public Shelf saveShelf(Shelf shelf) {
        return shelfRepository.save(shelf);
    }

    public void deleteShelf(Long id) {
        shelfRepository.deleteById(id);
    }
}

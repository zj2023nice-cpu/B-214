package com.warehouse.service;

import com.warehouse.dto.BatchDeleteFailureDetail;
import com.warehouse.dto.BatchDeleteItemResult;
import com.warehouse.dto.BatchDeleteResult;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.Warehouse;
import com.warehouse.repository.InboundRecordRepository;
import com.warehouse.repository.OutboundRecordRepository;
import com.warehouse.repository.ShelfRepository;
import com.warehouse.repository.TransferRecordRepository;
import com.warehouse.repository.WarehouseInventoryRepository;
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
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final InboundRecordRepository inboundRecordRepository;
    private final OutboundRecordRepository outboundRecordRepository;
    private final TransferRecordRepository transferRecordRepository;

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

        List<Long> deletableIds = new ArrayList<>();
        boolean hasFailure = false;

        for (Long id : ids) {
            String failureReason = validateWarehouseDeletion(id);
            if (failureReason != null) {
                hasFailure = true;
                result.getItems().add(new BatchDeleteItemResult(id, false, failureReason));
                result.getFailures().add(new BatchDeleteFailureDetail(id, failureReason));
            } else {
                deletableIds.add(id);
                result.getItems().add(new BatchDeleteItemResult(id, true, "校验通过，允许删除"));
            }
        }

        result.setSuccessCount(deletableIds.size());
        result.setFailureCount(result.getFailures().size());

        if (hasFailure) {
            markRollbackForSuccessfulItems(result, "校验通过，但因本次存在失败项，整批未执行删除");
            result.setAllSucceeded(false);
            result.setRolledBack(true);
            result.setSummaryMessage(String.format("批量删除校验未通过：成功校验 %d 条，失败 %d 条，整批未执行删除", result.getSuccessCount(), result.getFailureCount()));
            return result;
        }

        warehouseRepository.deleteAllById(deletableIds);
        markSuccessForItems(result, "删除成功");
        result.setAllSucceeded(true);
        result.setRolledBack(false);
        result.setSummaryMessage(String.format("删除成功，共删除 %d 条记录", result.getSuccessCount()));
        return result;
    }

    private String validateWarehouseDeletion(Long id) {
        if (!warehouseRepository.existsById(id)) {
            return "仓库不存在";
        }
        if (shelfRepository.existsByWarehouseId(id)) {
            return "仓库下存在货架，不能删除";
        }
        if (warehouseInventoryRepository.existsByWarehouseId(id)) {
            return "仓库存在库存记录，不能删除";
        }
        if (inboundRecordRepository.existsByWarehouseId(id)) {
            return "仓库已被入库记录引用，不能删除";
        }
        if (outboundRecordRepository.existsByWarehouseId(id)) {
            return "仓库已被出库记录引用，不能删除";
        }
        if (transferRecordRepository.existsBySourceWarehouseIdOrTargetWarehouseId(id, id)) {
            return "仓库已被调拨记录引用，不能删除";
        }
        return null;
    }

    private void markRollbackForSuccessfulItems(BatchDeleteResult result, String reason) {
        for (BatchDeleteItemResult item : result.getItems()) {
            if (item.isSuccess()) {
                item.setReason(reason);
            }
        }
    }

    private void markSuccessForItems(BatchDeleteResult result, String reason) {
        for (BatchDeleteItemResult item : result.getItems()) {
            if (item.isSuccess()) {
                item.setReason(reason);
            }
        }
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

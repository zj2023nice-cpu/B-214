package com.warehouse.service;

import com.warehouse.entity.InventoryCheck;
import com.warehouse.entity.InventoryCheck.CheckStatus;
import com.warehouse.entity.InventoryCheckDetail;
import com.warehouse.entity.Material;
import com.warehouse.repository.InventoryCheckDetailRepository;
import com.warehouse.repository.InventoryCheckRepository;
import com.warehouse.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryCheckService {
    private final InventoryCheckRepository inventoryCheckRepository;
    private final InventoryCheckDetailRepository inventoryCheckDetailRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public InventoryCheck createCheck(String remark) {
        InventoryCheck check = new InventoryCheck();
        check.setSerialNo(generateSerialNo());
        check.setCheckTime(LocalDateTime.now());
        check.setStatus(CheckStatus.PENDING);
        check.setRemark(remark);

        List<Material> materials = materialRepository.findAll();
        List<InventoryCheckDetail> details = new ArrayList<>();
        for (Material material : materials) {
            InventoryCheckDetail detail = new InventoryCheckDetail();
            detail.setInventoryCheck(check);
            detail.setMaterial(material);
            detail.setSystemQuantity(material.getStockQuantity());
            detail.setActualQuantity(null);
            detail.setDifference(null);
            details.add(detail);
        }
        check.setDetails(details);

        return inventoryCheckRepository.save(check);
    }

    @Transactional
    public InventoryCheck submitCheckResult(Long checkId, List<InventoryCheckDetail> detailUpdates) {
        InventoryCheck check = inventoryCheckRepository.findById(checkId)
                .orElseThrow(() -> new RuntimeException("Inventory check not found"));

        if (check.getStatus() == CheckStatus.COMPLETED) {
            throw new RuntimeException("Inventory check already completed");
        }

        Map<Long, InventoryCheckDetail> existingDetails = check.getDetails().stream()
                .collect(Collectors.toMap(InventoryCheckDetail::getId, Function.identity()));

        for (InventoryCheckDetail update : detailUpdates) {
            InventoryCheckDetail existing = existingDetails.get(update.getId());
            if (existing != null && update.getActualQuantity() != null) {
                existing.setActualQuantity(update.getActualQuantity());
                existing.setDifference(update.getActualQuantity() - existing.getSystemQuantity());
                existing.setRemark(update.getRemark());
            }
        }

        boolean allCompleted = check.getDetails().stream()
                .allMatch(d -> d.getActualQuantity() != null);

        if (allCompleted) {
            check.setStatus(CheckStatus.COMPLETED);
            for (InventoryCheckDetail detail : check.getDetails()) {
                Material material = detail.getMaterial();
                material.setStockQuantity(detail.getActualQuantity());
                materialRepository.save(material);
            }
        } else {
            check.setStatus(CheckStatus.IN_PROGRESS);
        }

        return inventoryCheckRepository.save(check);
    }

    public List<InventoryCheck> getAllChecks() {
        return inventoryCheckRepository.findAll();
    }

    public InventoryCheck getCheckById(Long id) {
        return inventoryCheckRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory check not found"));
    }

    private String generateSerialNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "IC-" + LocalDateTime.now().format(formatter);
    }
}

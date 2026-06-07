package com.warehouse.service;

import com.warehouse.entity.InboundRecord;
import com.warehouse.entity.Material;
import com.warehouse.entity.OutboundRecord;
import com.warehouse.repository.InboundRecordRepository;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.OutboundRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InboundRecordRepository inboundRecordRepository;
    private final OutboundRecordRepository outboundRecordRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public InboundRecord processInbound(InboundRecord record) {
        record.setInboundTime(LocalDateTime.now());
        
        // Update stock
        Material material = materialRepository.findById(record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material not found"));
        
        material.setStockQuantity(material.getStockQuantity() + record.getQuantity());
        materialRepository.save(material);
        
        record.setMaterial(material); // Ensure relationship
        return inboundRecordRepository.save(record);
    }

    @Transactional
    public OutboundRecord processOutbound(OutboundRecord record) {
        record.setOutboundTime(LocalDateTime.now());
        
        // Update stock
        Material material = materialRepository.findById(record.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material not found"));
        
        if (material.getStockQuantity() < record.getQuantity()) {
            throw new RuntimeException("Insufficient stock! Current: " + material.getStockQuantity());
        }
        
        material.setStockQuantity(material.getStockQuantity() - record.getQuantity());
        materialRepository.save(material);
        
        record.setMaterial(material); // Ensure relationship
        return outboundRecordRepository.save(record);
    }

    public List<InboundRecord> getAllInboundRecords() {
        return inboundRecordRepository.findAll();
    }

    public List<OutboundRecord> getAllOutboundRecords() {
        return outboundRecordRepository.findAll();
    }
}

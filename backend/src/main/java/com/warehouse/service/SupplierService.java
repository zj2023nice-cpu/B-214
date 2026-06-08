package com.warehouse.service;

import com.warehouse.entity.Supplier;
import com.warehouse.exception.DeleteConflictException;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier createSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(Long id, Supplier supplier) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        if (supplier.getName() != null) existing.setName(supplier.getName());
        if (supplier.getContactPerson() != null) existing.setContactPerson(supplier.getContactPerson());
        if (supplier.getPhone() != null) existing.setPhone(supplier.getPhone());
        if (supplier.getAddress() != null) existing.setAddress(supplier.getAddress());
        return supplierRepository.save(existing);
    }

    public void deleteSupplier(Long id) {
        long count = materialRepository.countBySupplierId(id);
        if (count > 0) {
            throw new DeleteConflictException("供应商", count);
        }
        supplierRepository.deleteById(id);
    }

    public long getMaterialCountBySupplierId(Long id) {
        return materialRepository.countBySupplierId(id);
    }
}

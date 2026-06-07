package com.warehouse.service;

import com.warehouse.entity.Category;
import com.warehouse.entity.Material;
import com.warehouse.entity.Supplier;
import com.warehouse.repository.CategoryRepository;
import com.warehouse.repository.MaterialRepository;
import com.warehouse.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    // Material Operations
    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    public Material getMaterialById(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new RuntimeException("Material not found"));
    }

    public Material saveMaterial(Material material) {
        return materialRepository.save(material);
    }

    public void deleteMaterial(Long id) {
        materialRepository.deleteById(id);
    }
    
    public List<Material> getLowStockMaterials(Integer threshold) {
        return materialRepository.findByStockQuantityLessThan(threshold);
    }

    // Category Operations
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }
    
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    // Supplier Operations
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }
    
    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }
}

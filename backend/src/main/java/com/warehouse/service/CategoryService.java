package com.warehouse.service;

import com.warehouse.entity.Category;
import com.warehouse.exception.DeleteConflictException;
import com.warehouse.repository.CategoryRepository;
import com.warehouse.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        if (category.getName() != null) existing.setName(category.getName());
        if (category.getDescription() != null) existing.setDescription(category.getDescription());
        return categoryRepository.save(existing);
    }

    public void deleteCategory(Long id) {
        long count = materialRepository.countByCategoryId(id);
        if (count > 0) {
            throw new DeleteConflictException("分类", count);
        }
        categoryRepository.deleteById(id);
    }

    public long getMaterialCountByCategoryId(Long id) {
        return materialRepository.countByCategoryId(id);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }
}

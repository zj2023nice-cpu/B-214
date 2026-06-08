package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.common.ApiResponse;
import com.warehouse.entity.Category;
import com.warehouse.entity.OperationType;
import com.warehouse.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<Category>> getAllCategories() {
        return ApiResponse.success(categoryService.getAllCategories());
    }

    @PostMapping
    @OperationLog(type = OperationType.CREATE, description = "新增分类", target = "分类")
    public ApiResponse<Category> createCategory(@RequestBody Category category) {
        return ApiResponse.success(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    @OperationLog(type = OperationType.UPDATE, description = "修改分类", target = "分类")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return ApiResponse.success(categoryService.updateCategory(id, category));
    }

    @GetMapping("/{id}/deletion-check")
    public ApiResponse<Map<String, Object>> checkDeletion(@PathVariable Long id) {
        long count = categoryService.getMaterialCountByCategoryId(id);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("materialCount", count);
        result.put("canDelete", count == 0);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @OperationLog(type = OperationType.DELETE, description = "删除分类", target = "分类")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success(null);
    }
}

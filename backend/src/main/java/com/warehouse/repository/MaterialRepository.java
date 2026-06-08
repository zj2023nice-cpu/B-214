package com.warehouse.repository;

import com.warehouse.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {
    List<Material> findByStockQuantityLessThan(Integer threshold);
    boolean existsByCode(String code);

    @Query("SELECT m FROM Material m WHERE m.stockQuantity < m.alertThreshold ORDER BY (m.alertThreshold - m.stockQuantity) DESC")
    List<Material> findAlertMaterials();

    @Query("SELECT m FROM Material m WHERE m.stockQuantity < m.alertThreshold AND m.category.id = :categoryId ORDER BY (m.alertThreshold - m.stockQuantity) DESC")
    List<Material> findAlertMaterialsByCategory(@Param("categoryId") Long categoryId);

    long countByCategoryId(Long categoryId);

    long countBySupplierId(Long supplierId);
}

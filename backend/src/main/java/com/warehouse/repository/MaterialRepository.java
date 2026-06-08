package com.warehouse.repository;

import com.warehouse.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {
    List<Material> findByStockQuantityLessThan(Integer threshold);
    boolean existsByCode(String code);
}

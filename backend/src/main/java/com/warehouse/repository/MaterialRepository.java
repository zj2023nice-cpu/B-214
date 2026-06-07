package com.warehouse.repository;

import com.warehouse.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByStockQuantityLessThan(Integer threshold);
}

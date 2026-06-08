package com.warehouse.repository;

import com.warehouse.entity.InventoryCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {
}

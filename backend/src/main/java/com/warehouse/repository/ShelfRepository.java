package com.warehouse.repository;

import com.warehouse.entity.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    List<Shelf> findByWarehouseId(Long warehouseId);
}

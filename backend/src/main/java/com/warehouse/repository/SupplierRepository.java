package com.warehouse.repository;

import com.warehouse.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("SELECT AVG(sr.rating) FROM SupplierRating sr WHERE sr.supplier.id = :supplierId")
    Double findAverageRatingBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT s.id, AVG(sr.rating) FROM Supplier s LEFT JOIN SupplierRating sr ON sr.supplier.id = s.id GROUP BY s.id")
    List<Object[]> findAllWithAverageRating();
}

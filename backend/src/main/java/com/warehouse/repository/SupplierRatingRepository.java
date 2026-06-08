package com.warehouse.repository;

import com.warehouse.entity.SupplierRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRatingRepository extends JpaRepository<SupplierRating, Long> {

    List<SupplierRating> findBySupplierIdOrderByRatingTimeDesc(Long supplierId);

    @Query("SELECT AVG(sr.rating) FROM SupplierRating sr WHERE sr.supplier.id = :supplierId")
    Double findAverageRatingBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT COUNT(sr) FROM SupplierRating sr WHERE sr.supplier.id = :supplierId")
    Long countBySupplierId(@Param("supplierId") Long supplierId);

    boolean existsByUserIdAndSupplierId(Long userId, Long supplierId);

    List<SupplierRating> findTop3BySupplierIdOrderByRatingTimeDesc(Long supplierId);

    void deleteByUserIdAndSupplierId(Long userId, Long supplierId);

    @Query("SELECT sr.supplier.id, AVG(sr.rating) FROM SupplierRating sr GROUP BY sr.supplier.id")
    List<Object[]> findAverageRatingGroupedBySupplier();
}

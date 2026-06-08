package com.warehouse.repository;

import com.warehouse.entity.SupplierRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRatingRepository extends JpaRepository<SupplierRating, Long> {

    List<SupplierRating> findBySupplierIdOrderByRatingTimeDesc(Long supplierId);

    boolean existsByUserIdAndSupplierId(Long userId, Long supplierId);

    List<SupplierRating> findTop3BySupplierIdOrderByRatingTimeDesc(Long supplierId);

    @Query("SELECT sr.supplier.id, COUNT(sr) FROM SupplierRating sr WHERE sr.supplier.id IN :supplierIds GROUP BY sr.supplier.id")
    List<Object[]> countBySupplierIdIn(@Param("supplierIds") List<Long> supplierIds);

    List<SupplierRating> findBySupplierIdInOrderByRatingTimeDesc(List<Long> supplierIds);
}

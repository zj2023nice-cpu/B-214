package com.warehouse.repository;

import com.warehouse.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    @Query("SELECT o FROM OperationLog o WHERE " +
           "(:username IS NULL OR o.username LIKE %:username%) AND " +
           "(:operationType IS NULL OR o.operationType = :operationType) AND " +
           "(:startTime IS NULL OR o.operationTime >= :startTime) AND " +
           "(:endTime IS NULL OR o.operationTime <= :endTime) " +
           "ORDER BY o.operationTime DESC")
    Page<OperationLog> findByFilters(
            @Param("username") String username,
            @Param("operationType") String operationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    @Modifying
    @Query("DELETE FROM OperationLog o WHERE o.operationTime < :cutoffTime")
    int deleteByOperationTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}

package com.warehouse.repository;

import com.warehouse.entity.InboundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long> {

    @Query("SELECT FUNCTION('DATE', ir.inboundTime) AS d, SUM(ir.quantity) AS total " +
           "FROM InboundRecord ir " +
           "WHERE ir.inboundTime >= :startDate " +
           "GROUP BY FUNCTION('DATE', ir.inboundTime) " +
           "ORDER BY d")
    List<Object[]> findDailyInboundTotals(@Param("startDate") LocalDateTime startDate);
}

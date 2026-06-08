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

    @Query("SELECT ir.material.id, SUM(ir.quantity) " +
           "FROM InboundRecord ir " +
           "WHERE ir.inboundTime >= :start AND ir.inboundTime < :end " +
           "GROUP BY ir.material.id")
    List<Object[]> findMonthlyInboundByMaterial(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT ir FROM InboundRecord ir " +
           "WHERE ir.inboundTime BETWEEN :startDate AND :endDate " +
           "ORDER BY ir.inboundTime DESC")
    List<InboundRecord> findByInboundTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ir FROM InboundRecord ir " +
           "WHERE ir.inboundTime >= :startDate " +
           "ORDER BY ir.inboundTime DESC")
    List<InboundRecord> findByInboundTimeAfterDate(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT ir FROM InboundRecord ir " +
           "WHERE ir.inboundTime <= :endDate " +
           "ORDER BY ir.inboundTime DESC")
    List<InboundRecord> findByInboundTimeBeforeDate(@Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(ir.quantity), 0) FROM InboundRecord ir " +
           "WHERE ir.inboundTime >= :start AND ir.inboundTime < :end")
    Long sumTodayInboundQuantity(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

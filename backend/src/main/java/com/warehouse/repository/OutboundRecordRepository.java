package com.warehouse.repository;

import com.warehouse.entity.OutboundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboundRecordRepository extends JpaRepository<OutboundRecord, Long> {

    @Query("SELECT FUNCTION('DATE', obr.outboundTime) AS d, SUM(obr.quantity) AS total " +
           "FROM OutboundRecord obr " +
           "WHERE obr.outboundTime >= :startDate " +
           "GROUP BY FUNCTION('DATE', obr.outboundTime) " +
           "ORDER BY d")
    List<Object[]> findDailyOutboundTotals(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT obr.material.id, SUM(obr.quantity) " +
           "FROM OutboundRecord obr " +
           "WHERE obr.outboundTime >= :start AND obr.outboundTime < :end " +
           "GROUP BY obr.material.id")
    List<Object[]> findMonthlyOutboundByMaterial(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT obr FROM OutboundRecord obr " +
           "WHERE obr.outboundTime BETWEEN :startDate AND :endDate " +
           "ORDER BY obr.outboundTime DESC")
    List<OutboundRecord> findByOutboundTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

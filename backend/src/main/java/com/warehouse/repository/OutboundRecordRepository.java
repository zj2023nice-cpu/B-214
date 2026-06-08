package com.warehouse.repository;

import com.warehouse.entity.OutboundRecord;
import com.warehouse.entity.OutboundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OutboundRecordRepository extends JpaRepository<OutboundRecord, Long> {

    @Query("SELECT FUNCTION('DATE', obr.outboundTime) AS d, SUM(obr.quantity) AS total " +
           "FROM OutboundRecord obr " +
           "WHERE obr.outboundTime >= :startDate AND obr.status IN :statuses " +
           "GROUP BY FUNCTION('DATE', obr.outboundTime) " +
           "ORDER BY d")
    List<Object[]> findDailyOutboundTotalsByStatuses(@Param("startDate") LocalDateTime startDate,
                                                     @Param("statuses") Collection<OutboundStatus> statuses);

    @Query("SELECT obr.material.id, SUM(obr.quantity) " +
           "FROM OutboundRecord obr " +
           "WHERE obr.outboundTime >= :start AND obr.outboundTime < :end AND obr.status IN :statuses " +
           "GROUP BY obr.material.id")
    List<Object[]> findMonthlyOutboundByMaterialAndStatuses(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end,
                                                            @Param("statuses") Collection<OutboundStatus> statuses);

    @Query("SELECT obr FROM OutboundRecord obr " +
           "WHERE obr.status IN :statuses AND obr.outboundTime BETWEEN :startDate AND :endDate " +
           "ORDER BY obr.outboundTime DESC")
    List<OutboundRecord> findByStatusesAndOutboundTimeBetween(@Param("statuses") Collection<OutboundStatus> statuses,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT obr FROM OutboundRecord obr " +
           "WHERE obr.status IN :statuses AND obr.outboundTime >= :startDate " +
           "ORDER BY obr.outboundTime DESC")
    List<OutboundRecord> findByStatusesAndOutboundTimeAfterDate(@Param("statuses") Collection<OutboundStatus> statuses,
                                                                @Param("startDate") LocalDateTime startDate);

    @Query("SELECT obr FROM OutboundRecord obr " +
           "WHERE obr.status IN :statuses AND obr.outboundTime <= :endDate " +
           "ORDER BY obr.outboundTime DESC")
    List<OutboundRecord> findByStatusesAndOutboundTimeBeforeDate(@Param("statuses") Collection<OutboundStatus> statuses,
                                                                 @Param("endDate") LocalDateTime endDate);

    @Query("SELECT obr FROM OutboundRecord obr " +
           "WHERE obr.outboundTime BETWEEN :startDate AND :endDate " +
           "ORDER BY obr.outboundTime DESC")
    List<OutboundRecord> findByOutboundTimeBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT obr FROM OutboundRecord obr " +
           "WHERE obr.outboundTime >= :startDate " +
           "ORDER BY obr.outboundTime DESC")
    List<OutboundRecord> findByOutboundTimeAfterDate(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT obr FROM OutboundRecord obr " +
           "WHERE obr.outboundTime <= :endDate " +
           "ORDER BY obr.outboundTime DESC")
    List<OutboundRecord> findByOutboundTimeBeforeDate(@Param("endDate") LocalDateTime endDate);

    List<OutboundRecord> findByStatusInOrderByOutboundTimeDesc(Collection<OutboundStatus> statuses);

    List<OutboundRecord> findByStatusOrderByOutboundTimeDesc(OutboundStatus status);

    @Query("SELECT COALESCE(SUM(obr.quantity), 0) FROM OutboundRecord obr " +
           "WHERE obr.outboundTime >= :start AND obr.outboundTime < :end AND obr.status IN :statuses")
    Long sumTodayOutboundQuantity(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("statuses") Collection<OutboundStatus> statuses);
}

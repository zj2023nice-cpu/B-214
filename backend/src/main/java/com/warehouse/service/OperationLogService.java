package com.warehouse.service;

import com.warehouse.entity.OperationLog;
import com.warehouse.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Async
    @Transactional
    public void saveAsync(OperationLog operationLog) {
        try {
            operationLogRepository.save(operationLog);
        } catch (Exception e) {
            log.error("Failed to save operation log: {}", e.getMessage(), e);
        }
    }

    public Page<OperationLog> queryLogs(String username, String operationType,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return operationLogRepository.findByFilters(username, operationType, startTime, endTime, pageable);
    }

    @Transactional
    public int cleanExpiredLogs(int retainDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retainDays);
        int deleted = operationLogRepository.deleteByOperationTimeBefore(cutoffTime);
        log.info("Cleaned up {} operation logs older than {} days", deleted, retainDays);
        return deleted;
    }
}

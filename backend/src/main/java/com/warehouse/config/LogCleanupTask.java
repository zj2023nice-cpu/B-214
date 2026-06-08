package com.warehouse.config;

import com.warehouse.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogCleanupTask {

    private final OperationLogService operationLogService;

    private static final int RETAIN_DAYS = 90;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredLogs() {
        log.info("Starting operation log cleanup, retaining last {} days", RETAIN_DAYS);
        try {
            int deleted = operationLogService.cleanExpiredLogs(RETAIN_DAYS);
            log.info("Operation log cleanup completed, deleted {} records", deleted);
        } catch (Exception e) {
            log.error("Operation log cleanup failed: {}", e.getMessage(), e);
        }
    }
}

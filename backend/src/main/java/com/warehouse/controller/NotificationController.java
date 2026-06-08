package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
import com.warehouse.entity.Notification;
import com.warehouse.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<Notification>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(notificationService.getNotificationsByUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Notification> markAsRead(@PathVariable Long id) {
        return ApiResponse.success(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(null);
    }
}

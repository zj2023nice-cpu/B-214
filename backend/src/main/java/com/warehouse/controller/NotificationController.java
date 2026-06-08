package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
import com.warehouse.entity.Notification;
import com.warehouse.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
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
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(request);
        return ApiResponse.success(notificationService.getNotificationsByUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Notification> markAsRead(@PathVariable Long id) {
        return ApiResponse.success(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(null);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return (Long) userIdAttr;
        }
        throw new RuntimeException("未提供用户身份信息");
    }
}

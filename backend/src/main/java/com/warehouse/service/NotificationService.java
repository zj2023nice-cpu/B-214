package com.warehouse.service;

import com.warehouse.entity.Notification;
import com.warehouse.entity.User;
import com.warehouse.repository.NotificationRepository;
import com.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendNotificationToAllUsers(String title, String content, String type, String link) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setLink(link);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUser(user);
            notificationRepository.save(notification);
        }
    }

    @Async
    public void sendNotificationToAllUsersAsync(String title, String content, String type, String link) {
        try {
            sendNotificationToAllUsers(title, content, type, link);
        } catch (Exception e) {
            log.error("异步发送通知失败: title={}, error={}", title, e.getMessage(), e);
        }
    }

    @Transactional
    public void sendNotificationToUser(Long userId, String title, String content, String type, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setLink(link);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUser(user);
        notificationRepository.save(notification);
    }

    public Page<Notification> getNotificationsByUser(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}

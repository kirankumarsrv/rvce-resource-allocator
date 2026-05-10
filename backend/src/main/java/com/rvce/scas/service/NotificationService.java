package com.rvce.scas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvce.scas.dto.response.NotificationDto;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.Notification;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import com.rvce.scas.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing notifications
 * Handles: creation, retrieval, marking as read
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final ExamStudentRepository examStudentRepository;
    private final ExamSeatRepository examSeatRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    /**
     * Get all notifications for a user (paginated)
     */
    public Page<NotificationDto> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::toDto);
    }

    /**
     * Get unread notification count for a user
     */
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public NotificationDto markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user: " + userId);
        }

        notificationRepository.markAsRead(notificationId);
        auditService.log(userId, "NOTIFICATION_READ", "NOTIFICATION", notificationId);

        return notificationRepository.findById(notificationId).map(this::toDto).orElse(null);
    }

    /**
     * Mark all unread notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
        auditService.log(userId, "ALL_NOTIFICATIONS_READ", "USER", userId);
    }

    /**
     * Create a notification (internal use)
     * Called by NotificationOrchestrator
     */
    @Transactional
    public NotificationDto createNotification(
        UUID userId,
        String notificationType,
        String title,
        String body,
        Object payload,
        UUID refEntityId,
        String refEntityType,
        UUID batchId
    ) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setPayload(objectMapper.valueToTree(payload));
        notification.setRefEntityId(refEntityId);
        notification.setRefEntityType(refEntityType);
        notification.setBatchId(batchId);
        notification.setIsRead(false);
        notification.setDeliveryStatus("PENDING");

        Notification saved = notificationRepository.save(notification);
        return toDto(saved);
    }

    /**
     * Convert entity to DTO
     */
    private NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setNotificationId(notification.getNotificationId());
        dto.setNotificationType(notification.getNotificationType());
        dto.setTitle(notification.getTitle());
        dto.setBody(notification.getBody());
        dto.setPayload(notification.getPayload());
        dto.setIsRead(notification.getIsRead());
        dto.setDeliveryStatus(notification.getDeliveryStatus());
        dto.setRefEntityId(notification.getRefEntityId());
        dto.setRefEntityType(notification.getRefEntityType());
        dto.setReadAt(notification.getReadAt());
        dto.setSentAt(notification.getSentAt());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}

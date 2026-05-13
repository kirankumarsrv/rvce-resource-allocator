package com.rvce.scas.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity — stores in-app notifications for users
 * Includes exam seating notifications, system alerts, etc.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_unread", columnList = "user_id, is_read"),
    @Index(name = "idx_created_at", columnList = "created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "notif_type", nullable = false, length = 30)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "payload", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "delivery_status", nullable = false, length = 10)
    private String deliveryStatus;

    @Column(name = "fcm_message_id", length = 200)
    private String fcmMessageId;

    @Column(name = "ref_entity_id")
    private UUID refEntityId;

    @Column(name = "ref_entity_type", length = 20)
    private String refEntityType;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = Instant.now();
    }

    @PrePersist
    private void prePersist() {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (deliveryStatus == null) {
            deliveryStatus = "PENDING";
        }
    }
}

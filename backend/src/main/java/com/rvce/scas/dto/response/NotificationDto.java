package com.rvce.scas.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for notification response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID notificationId;
    private String notificationType;
    private String title;
    private String body;
    private JsonNode payload;
    private Boolean isRead;
    private String deliveryStatus;
    private UUID refEntityId;
    private String refEntityType;
    private Instant readAt;
    private Instant sentAt;
    private Instant createdAt;
}

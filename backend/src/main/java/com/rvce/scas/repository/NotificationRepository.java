package com.rvce.scas.repository;

import com.rvce.scas.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Notification persistence
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /**
     * Find all unread notifications for a user, paginated
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all notifications for a user, paginated
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Count unread notifications for a user
     */
    long countByUserIdAndIsReadFalse(UUID userId);

    /**
     * Find all unread notifications for a user
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * Find notifications by batch ID
     */
    List<Notification> findByBatchId(UUID batchId);

    /**
     * Find notifications by reference entity
     */
    List<Notification> findByRefEntityIdAndRefEntityType(UUID refEntityId, String refEntityType);

    /**
     * Mark a single notification as read
     */
    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.deliveryStatus = 'READ' WHERE n.notificationId = :id")
    void markAsRead(@Param("id") UUID notificationId);

    /**
     * Mark all unread notifications as read for a user
     */
    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.deliveryStatus = 'READ' WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId);
}

package com.rvce.scas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a password reset token.
 * Used for "forgot password" flows where users receive an email link to reset their password.
 *
 * <p><strong>Table:</strong> {@code password_reset_tokens}</p>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ul>
 *   <li>Created when user requests password reset</li>
 *   <li>Validated when user submits new password</li>
 *   <li>Expires after a configurable duration (default 24 hours)</li>
 *   <li>Marked as used after successful password change</li>
 * </ul>
 */
@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id")
    private UUID tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hex of the raw token

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

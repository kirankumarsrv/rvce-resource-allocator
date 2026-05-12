package com.rvce.scas.repository;

import com.rvce.scas.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset token persistence and queries.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find an unused and non-expired reset token by its hash.
     */
    @Query("""
        SELECT t FROM PasswordResetToken t
        WHERE t.tokenHash = :tokenHash
          AND t.used = false
          AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    Optional<PasswordResetToken> findValidTokenByHash(@Param("tokenHash") String tokenHash);

    /**
     * Find any reset token by its hash (used or expired).
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}

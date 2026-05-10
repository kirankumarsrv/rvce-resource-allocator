package com.rvce.scas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a system user with authentication and authorization information.
 *
 * <p><strong>Table:</strong> {@code users}</p>
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *   <li>Store user authentication credentials (email, password hash)</li>
 *   <li>Track account status (active/inactive)</li>
 *   <li>Maintain associations with assigned roles</li>
 *   <li>Support distributed account lockout via Redis (see {@code lockedUntil})</li>
 * </ul>
 *
 * <p><strong>Key Fields:</strong></p>
 * <ul>
 *   <li>{@code userId}: Primary key, UUID generated automatically</li>
 *   <li>{@code email}: Unique identifier for login, must be lowercase-normalized</li>
 *   <li>{@code passwordHash}: Bcrypt or similar strong hash (never plaintext)</li>
 *   <li>{@code active}: Flag to disable user without deletion (soft deactivation)</li>
 *   <li>{@code lockedUntil}: Future timestamp for account lockout (currently unused in favor of Redis)</li>
 *   <li>{@code userRoles}: Bidirectional many-to-many relationship with roles</li>
 * </ul>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>One-to-Many with {@link UserRole}: User can have multiple roles (ADMIN, STUDENT, TEACHER)</li>
 *   <li>Through {@code UserRole}, indirectly relates to {@link Role} and {@link Permission}</li>
 * </ul>
 *
 * @author RVCE SCAS Team
 * @see UserRole
 * @see Role
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "usn", length = 20)
    private String usn;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "failed_login_count", nullable = false)
    private short failedLoginCount;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private Set<UserRole> userRoles = new HashSet<>();
}

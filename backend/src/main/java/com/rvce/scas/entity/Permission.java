package com.rvce.scas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Entity representing a fine-grained permission in a resource-action model.
 *
 * <p><strong>Table:</strong> {@code permissions}</p>
 *
 * <p><strong>Purpose:</strong> Permissions are the finest granularity of authorization.
 * Each permission is defined by a (resource, action) pair. For example,
 * resource="EXAM", action="VIEW" creates a permission to view exam data.</p>
 *
 * <p><strong>Authorization in JWT:</strong> When generating tokens, permissions are
 * converted to strings like "EXAM_VIEW" and included in JWT claims for use by
 * Spring Security's {@code hasAuthority()} checks.</p>
 *
 * @author RVCE SCAS Team
 * @see Role
 * @see RolePermission
 * @see UserRole
 */
@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id")
    private UUID permissionId;

    @Column(name = "resource", nullable = false)
    private String resource;

    @Column(name = "action", nullable = false)
    private String action;
}

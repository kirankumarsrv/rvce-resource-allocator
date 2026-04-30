package com.rvce.scas.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Join entity representing the many-to-many relationship between Role and Permission.
 *
 * <p><strong>Table:</strong> {@code role_permissions}</p>
 *
 * <p><strong>Purpose:</strong> Models M:N relationship where a role can have multiple
 * fine-grained permissions and a permission can be assigned to multiple roles.
 * This is the second dimension of the RBAC authorization hierarchy.</p>
 *
 * <p><strong>Primary Key:</strong> Composite key using RolePermissionId (roleId + permissionId)
 * ensures no duplicate permission assignments for the same role.</p>
 *
 * @author RVCE SCAS Team
 * @see Role
 * @see Permission
 * @see RolePermissionId
 * @see UserRole
 */
@Getter
@Setter
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    private Permission permission;
}

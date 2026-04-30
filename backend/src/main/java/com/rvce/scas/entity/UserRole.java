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
 * Join entity representing the many-to-many relationship between User and Role.
 *
 * <p><strong>Table:</strong> {@code user_roles}</p>
 *
 * <p><strong>Purpose:</strong> Models M:N relationship where a user can have multiple
 * roles and a role can be assigned to multiple users. This is the core of the RBAC system.</p>
 *
 * <p><strong>Primary Key:</strong> Composite key using UserRoleId (userId + roleId)
 * ensures no duplicate role assignments for the same user.</p>
 *
 * @author RVCE SCAS Team
 * @see User
 * @see Role
 * @see UserRoleId
 */
@Getter
@Setter
@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;
}

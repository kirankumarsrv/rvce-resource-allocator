package com.rvce.scas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a system role with associated permissions.
 *
 * <p><strong>Table:</strong> {@code roles}</p>
 *
 * <p><strong>Examples of Roles:</strong></p>
 * <ul>
 *   <li>{@code ADMIN}: Full system access</li>
 *   <li>{@code TEACHER}: Can view and manage exam data</li>
 *   <li>{@code STUDENT}: Can view exam schedules and seat allocations</li>
 *   <li>{@code TIMETABLE_OPERATOR}: Can manage timetable data</li>
 * </ul>
 *
 * <p><strong>Design Pattern:</strong> Roles are the coarse-grained layer in Role-Based
 * Access Control (RBAC). Each role contains zero or more fine-grained Permissions.
 * Users are assigned roles via the UserRole join table.</p>
 *
 * @author RVCE SCAS Team
 * @see User
 * @see UserRole
 * @see Permission
 * @see RolePermission
 */
@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    private Set<RolePermission> rolePermissions = new HashSet<>();
}

package com.rvce.scas.repository;

import com.rvce.scas.entity.UserRole;
import com.rvce.scas.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository for user-role assignments.
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    /**
     * Checks if a user has a specific role assigned.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return true if the user has the role, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END FROM UserRole ur WHERE ur.user.userId = :userId AND ur.role.roleId = :roleId")
    boolean existsByUser_UserIdAndRole_RoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
}
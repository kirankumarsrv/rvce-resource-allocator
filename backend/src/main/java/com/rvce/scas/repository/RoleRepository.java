package com.rvce.scas.repository;

import com.rvce.scas.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Role entity persistence and querying.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {
    /**
     * Finds a role by name.
     *
     * @param name the role name
     * @return Optional containing the Role if found, empty Optional otherwise
     */
    Optional<Role> findByName(String name);
}
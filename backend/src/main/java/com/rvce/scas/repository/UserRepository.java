package com.rvce.scas.repository;

import com.rvce.scas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Spring Data JPA repository for User entity persistence and querying.
 *
 * <p><strong>Purpose:</strong> Provides database access for User entities.
 * Extends JpaRepository for standard CRUD operations plus custom queries.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li>Load users by UUID (primary key)</li>
 *   <li>Find users by email (case-insensitive for login)</li>
 *   <li>Save and update user records</li>
 *   <li>Delete users (soft deletes via active flag recommended)</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 *   // Find by primary key
 *   Optional&lt;User&gt; user = userRepository.findById(userId);
 *
 *   // Find by email (case-insensitive)
 *   Optional&lt;User&gt; user = userRepository.findByEmailIgnoreCase("student@example.com");
 *
 *   // Eager loading of roles happens via OneToMany(fetch=EAGER) in User entity
 *   User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
 *   Set&lt;UserRole&gt; roles = user.getUserRoles(); // Already loaded
 * </pre>
 *
 * <p><strong>Transaction Context:</strong></p>
 * <ul>
 *   <li>Lazy-loaded relationships (e.g., User.userRoles) are eagerly fetched
 *       due to FetchType.EAGER in the entity definition</li>
 *   <li>Callers should be in a @Transactional context to access loaded relationships</li>
 * </ul>
 *
 * @author RVCE SCAS Team
 * @see User
 * @see UserRole
 * @see UserDetailsServiceImpl
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Finds a user by email address (case-insensitive).
     *
     * <p><strong>Purpose:</strong> Supports case-insensitive email lookup for login,
     * allowing users to login with any case variation of their email address.
     * For example, "User@Example.COM" and "user@example.com" will find the same account.</p>
     *
     * <p><strong>Usage:</strong> Called by UserDetailsServiceImpl during login to
     * resolve credentials. Also used during user registration to check email uniqueness.</p>
     *
     * <p><strong>Database Note:</strong> Relies on database's case-insensitive string
     * comparison (available in most modern databases). Ensure database collation
     * supports case-insensitive comparison for this method to work correctly.</p>
     *
     * @param email the user's email address (any case variation accepted)
     * @return Optional containing the User if found, empty Optional otherwise
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Finds users by a set of USN values.
     *
     * <p><strong>Purpose:</strong> Supports bulk lookup of users by their University Seat Numbers
     * for exam student linking operations.</p>
     *
     * @param usns the set of USN values to search for
     * @return List of User entities matching the provided USNs
     */
    List<User> findByUsnIn(Set<String> usns);

    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.userRoles ur
        JOIN ur.role r
                WHERE LOWER(r.name) = LOWER(:roleName)
                    AND u.active = true
        ORDER BY u.name ASC
        """)
    List<User> findAllByRoleName(@Param("roleName") String roleName);
}

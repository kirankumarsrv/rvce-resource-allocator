package com.rvce.scas.repository;

import com.rvce.scas.entity.User;
import com.rvce.scas.entity.UserRole;
import com.rvce.scas.security.EmailHashUtil;
import com.rvce.scas.security.UserDetailsServiceImpl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailHash(String emailHash);

    @Query(value = "SELECT * FROM users WHERE lower(email) = lower(:email)", nativeQuery = true)
    Optional<User> findByPlainEmailIgnoreCase(@Param("email") String email);

    default Optional<User> findByEmailIgnoreCase(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String emailHash = EmailHashUtil.hashEmail(email);
        Optional<User> user = findByEmailHash(emailHash);
        return user.isPresent() ? user : findByPlainEmailIgnoreCase(email);
    }

    List<User> findByUsnIn(Set<String> usns);

    Optional<User> findByName(String name);

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
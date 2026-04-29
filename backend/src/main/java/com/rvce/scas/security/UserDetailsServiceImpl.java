package com.rvce.scas.security;

import com.rvce.scas.entity.User;
import com.rvce.scas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j // Lombok annotation for logging
@Service           // business logic implementation 
@RequiredArgsConstructor // generates constructor for final fields, used for dependency injection
public class UserDetailsServiceImpl implements UserDetailsService { // Spring Security interface for loading user-specific data during authentication

        /*
         * Detailed conceptual notes (for beginners):
         *
         * - Purpose: This class implements Spring Security's `UserDetailsService` which is the
         *   standard extension point Spring uses to load user information during authentication.
         *   When `AuthenticationManager` needs to authenticate credentials (email + password), it
         *   calls `loadUserByUsername` to obtain a `UserDetails` instance that contains the stored
         *   password hash and granted authorities.
         *
         * - @Service: declares this class as a Spring-managed bean. It will be discovered and
         *   injected into other beans (for example the `DaoAuthenticationProvider`).
         *
         * - @RequiredArgsConstructor: Lombok annotation that creates a constructor for `final`
         *   fields. It allows Spring to `@Autowired` the `UserRepository` through constructor injection.
         *
         * - @Transactional(readOnly = true): opens a read-only transactional context for the
         *   duration of `loadUserByUsername`. This is important because JPA/Hibernate may lazily
         *   load related collections (roles, permissions). Without an open transaction the
         *   lazy collections would throw `LazyInitializationException` when accessed outside the
         *   DAO method. `readOnly=true` hints the DB that no write locks are needed and can
         *   yield small performance improvements for read-heavy paths like authentication.
         *
         * - Email as identity: Although the method signature says "username", the application
         *   uses `email` as the unique login identifier. The DB uses case-insensitive columns
         *   (CITEXT) and the repository method `findByEmailIgnoreCase` enforces that.
         *
         * - Authorities vs Roles: Spring treats `ROLE_*` authorities specially for `hasRole("X")`
         *   checks. This implementation adds both `ROLE_<name>` authorities for role checks and
         *   fine-grained `RESOURCE_ACTION` authorities (e.g., `TIMETABLE_WRITE`) to support
         *   permission-based checks (`hasAuthority("TIMETABLE_WRITE")`). Both forms live in the
         *   same collection of `GrantedAuthority` objects on the principal.
         */
    // T-005 DECISION [3]: Spring calls this "username" historically, but we treat it as email.
    // Email is chosen as stable, user-known, and globally unique identifier.
    private final UserRepository userRepository; // JPA repository for User entity, used to fetch user data from the database

    @Override // method from UserDetailsService interface, called by Spring Security during authentication
    // T-005 DECISION [2]: Keep transactional context open for lazy role/permission graph reads.
    // readOnly=true reduces DB overhead on auth read path and avoids accidental writes.
    @Transactional(readOnly = true) // ensures that the method runs within a transactional context, with read-only optimizations
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Lookup is case-insensitive to align with email semantics and DB constraints.
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    // REVIEW-NOTE: this is only a server log; API response still uses generic invalid-credentials message.
                    log.warn("Auth attempt for unknown email: {}", email);
                    return new UsernameNotFoundException("No user found with email: " + email);
                });

        // Build principal with both coarse role and fine-grained permission authorities.
        return new ScasPrincipal(user, buildAuthorities(user));
    }

    private Collection<GrantedAuthority> buildAuthorities(User user) {
        // T-005 DECISION [1]: include ROLE_* and RESOURCE_ACTION authorities in one set.
        // Example: ROLE_TEACHER + TIMETABLE_WRITE.
        Set<GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .flatMap(role -> {
                    Set<GrantedAuthority> roleAuthorities = new HashSet<>();
                    // Coarse-grained role authority for hasRole("...") checks.
                    roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    // Fine-grained permissions for hasAuthority("RESOURCE_ACTION") checks.
                    role.getRolePermissions().stream()
                            .map(rp -> rp.getPermission())
                            .map(perm -> perm.getResource().toUpperCase() + "_" + perm.getAction().toUpperCase())
                            .map(SimpleGrantedAuthority::new)
                            .forEach(roleAuthorities::add);
                    return roleAuthorities.stream();
                })
                // Set removes duplicates across multi-role overlap.
                .collect(Collectors.toSet());

        log.debug("Loaded {} authorities for user {}", authorities.size(), user.getEmail());
        return authorities;
    }
}

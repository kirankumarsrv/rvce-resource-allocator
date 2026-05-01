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

/**
 * Spring Security UserDetailsService implementation for SCAS system.
 *
 * <p><strong>Purpose:</strong> Loads user information from the database during
 * authentication and converts it into Spring Security's UserDetails format.
 * Used by the AuthenticationManager's authentication provider.</p>
 *
 * <p><strong>Key Flow:</strong></p>
 * <ol>
 *   <li>Client submits credentials to /api/auth/login</li>
 *   <li>AuthenticationManager calls loadUserByUsername(email)</li>
 *   <li>UserDetailsServiceImpl queries database for the user</li>
 *   <li>User's roles and permissions are loaded from database</li>
 *   <li>ScasPrincipal is returned with user data + authorities</li>
 *   <li>Password encoder compares submitted password with stored hash</li>
 *   <li>If valid, JWT tokens are generated with user identity + authorities</li>
 * </ol>
 *
 * <p><strong>Authority Building:</strong> Combines two types of authorities:
 * <ul>
 *   <li>Coarse-grained roles: "ROLE_ADMIN", "ROLE_STUDENT", etc.</li>
 *   <li>Fine-grained permissions: "EXAM_VIEW", "ROOM_EDIT", etc.</li>
 * </ul>
 * Both types are included in JWT claims and available for authorization checks.</p>
 *
 * @author RVCE SCAS Team
 * @see UserDetailsService
 * @see ScasPrincipal
 * @see UserRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * Loads a user by email (username) and builds a Spring Security principal.
     *
     * <p><strong>Process:</strong></p>
     * <ol>
     *   <li>Query database for user by email (case-insensitive)</li>
     *   <li>Load associated roles via eager fetching (OneToMany.EAGER in User entity)</li>
     *   <li>Build authorities from roles and permissions</li>
     *   <li>Return ScasPrincipal for authentication</li>
     * </ol>
     *
     * <p><strong>Transaction Context:</strong> Method is @Transactional(readOnly=true)
     * to ensure lazy-loaded relationships (role permissions) can be accessed without
     * causing LazyInitializationException.</p>
     *
     * <p><strong>Error Handling:</strong> Logs authentication attempts for users that
     * don't exist to aid security monitoring and debugging.</p>
     *
     * @param email user's login identifier (email address)
     * @return Spring Security UserDetails backed by User entity
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Auth attempt for unknown email: {}", email);
                    return new UsernameNotFoundException("No user found with email: " + email);
                });

        return new ScasPrincipal(user, buildAuthorities(user));
    }

    /**
     * Builds granted authorities from user's roles and their permissions.
     *
     * <p><strong>Authority Types Generated:</strong></p>
     * <ul>
     *   <li>Role authority: "ROLE_" + role name (e.g., "ROLE_ADMIN")</li>
     *   <li>Permission authority: "RESOURCE_ACTION" (e.g., "EXAM_VIEW")</li>
     * </ul>
     *
     * <p><strong>Process:</strong></p>
     * <ol>
     *   <li>Iterate through user's assigned roles</li>
     *   <li>For each role, create a "ROLE_*" authority</li>
     *   <li>For each permission in the role, create a "RESOURCE_ACTION" authority</li>
     *   <li>Deduplicate authorities across multiple roles (Set used)</li>
     *   <li>Return complete authority collection</li>
     * </ol>
     *
     * <p><strong>Example Output:</strong>
     * For a user with role "ADMIN" having permissions "EXAM_VIEW", "ROOM_EDIT":
     * <pre>
     *   [
     *     SimpleGrantedAuthority("ROLE_ADMIN"),
     *     SimpleGrantedAuthority("EXAM_VIEW"),
     *     SimpleGrantedAuthority("ROOM_EDIT")
     *   ]
     * </pre>
     *
     * <p><strong>Usage in Authorization:</strong> These authorities are used by:
     * <ul>
     *   <li>Spring Security's {@code hasRole()}, {@code hasAuthority()}, etc.</li>
     *   <li>Method-level @Secured/@PreAuthorize annotations</li>
     *   <li>JWT token claims for stateless request authorization</li>
     * </ul>
     *
     * @param user the authenticated user entity from database
     * @return collection of GrantedAuthority for the user
     */
    private Collection<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .flatMap(role -> {
                    Set<GrantedAuthority> roleAuthorities = new HashSet<>();
                    // Coarse-grained role authority
                    roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    // Fine-grained permission authorities
                    role.getRolePermissions().stream()
                            .map(rp -> rp.getPermission())
                            .map(perm -> perm.getResource().toUpperCase() + "_" + perm.getAction().toUpperCase())
                            .map(SimpleGrantedAuthority::new)
                            .forEach(roleAuthorities::add);
                    return roleAuthorities.stream();
                })
                .collect(Collectors.toSet());

        log.debug("Loaded {} authorities for user {}", authorities.size(), user.getEmail());
        return authorities;
    }
}

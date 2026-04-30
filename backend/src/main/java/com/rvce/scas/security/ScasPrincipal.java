package com.rvce.scas.security;

import com.rvce.scas.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * UserDetails implementation representing an authenticated user from the database.
 *
 * <p><strong>Purpose:</strong> Implements Spring Security's UserDetails interface
 * to provide user credentials and authorities for authentication and authorization.
 * Used during form-based login flow, not JWT-based requests.</p>
 *
 * <p><strong>Data Source:</strong> Populated from User entity and associated
 * roles/permissions loaded from database. Lazy-loaded relationships are accessed
 * within transactional context to avoid LazyInitializationException.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li>Provide username (email) and password hash to AuthenticationManager</li>
 *   <li>Indicate if account is enabled/locked/active</li>
 *   <li>Supply authorities for authorization checks</li>
 *   <li>Support JWT token generation with user identity and permissions</li>
 * </ul>
 *
 * <p><strong>Usage Flow:</strong></p>
 * <ol>
 *   <li>UserDetailsServiceImpl loads User from database</li>
 *   <li>User's roles and permissions are converted to GrantedAuthority list</li>
 *   <li>ScasPrincipal wraps both user data and authorities</li>
 *   <li>AuthenticationManager uses passwordHash for password comparison</li>
 *   <li>JWT is generated with userId, email, and authorities</li>
 * </ol>
 *
 * <p><strong>Compared to JwtPrincipal:</strong></p>
 * <ul>
 *   <li>ScasPrincipal: Database-backed, used during login (stateful)</li>
 *   <li>JwtPrincipal: Token-backed, used for API requests (stateless)</li>
 * </ul>
 *
 * @author RVCE SCAS Team
 * @see JwtPrincipal
 * @see UserDetailsServiceImpl
 * @see User
 */
@Getter
public class ScasPrincipal implements UserDetails {

    /**
     * Unique identifier of the user.
     * Used as the 'sub' (subject) claim in generated JWT tokens.
     */
    private final UUID userId;

    /**
     * User's email address (login identifier).
     * Returned by getUsername() for Spring Security compatibility.
     * Must be unique in the system.
     */
    private final String email;

    /**
     * Bcrypt hash of the user's password.
     * Used by AuthenticationManager's password encoder for credential comparison.
     * Never the plaintext password.
     */
    private final String passwordHash;

    /**
     * Boolean indicating if the user account is active/enabled.
     * {@code true} = account enabled, {@code false} = account disabled.
     * Returned by isEnabled() for Spring Security account status.
     */
    private final boolean active;

    /**
     * Boolean indicating if the account is locked.
     * Derived from the User entity's lockedUntil timestamp.
     * {@code true} if lockedUntil is in the future (currently locked).
     */
    private final boolean accountLocked;

    /**
     * Granted authorities for the user (roles + permissions).
     * Examples: ["ROLE_ADMIN", "EXAM_VIEW", "ROOM_EDIT"]
     * Determined by UserDetailsServiceImpl.buildAuthorities().
     */
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructs a ScasPrincipal from a User entity and authorities.
     *
     * @param user the authenticated User entity from database
     * @param authorities the collection of GrantedAuthority for the user
     */
    public ScasPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.active = user.isActive();
        this.accountLocked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
        this.authorities = authorities;
    }

    /**
     * Returns the username (email) for Spring Security.
     * @return user's email address
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Returns the password hash for Spring Security password comparison.
     * @return bcrypt password hash
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Indicates if the user account is enabled.
     * @return {@code true} if active, {@code false} if disabled
     */
    @Override
    public boolean isEnabled() {
        return active;
    }

    /**
     * Indicates if the account is not locked.
     * @return {@code true} if not locked, {@code false} if locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    /**
     * Indicates if the account is not expired.
     * Currently always returns true (accounts don't expire).
     * @return {@code true}
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates if the credentials are not expired.
     * Currently always returns true (credentials don't expire).
     * @return {@code true}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Returns the authorities (roles + permissions) for the user.
     * @return collection of GrantedAuthority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}

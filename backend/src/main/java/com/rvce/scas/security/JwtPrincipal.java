package com.rvce.scas.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

/**
 * Authenticated principal exposed to Spring Security and controller methods.
 *
 * <p><strong>Purpose:</strong> Lightweight representation of an authenticated user
 * extracted from a validated JWT token. Used in controller methods via
 * {@code @AuthenticationPrincipal JwtPrincipal principal} annotation.</p>
 *
 * <p><strong>Data Source:</strong> Populated from JWT claims after successful token
 * validation by JwtAuthFilter. Does NOT perform database lookups.</p>
 *
 * <p><strong>Usage in Controllers:</strong></p>
 * <pre>
 *   @PostMapping("/logout")
 *   public ResponseEntity&lt;Void&gt; logout(
 *       @AuthenticationPrincipal JwtPrincipal principal) {
 *       UUID userId = principal.getUserId();
 *       String email = principal.getEmail();
 *       // Use principal data without DB lookup
 *   }
 * </pre>
 *
 * <p><strong>Compared to ScasPrincipal:</strong></p>
 * <ul>
 *   <li>JwtPrincipal: Used during JWT-based requests (stateless, from token claims)</li>
 *   <li>ScasPrincipal: Used during form-based login (stateful, from DB lookup)</li>
 * </ul>
 *
 * @author RVCE SCAS Team
 * @see ScasPrincipal
 * @see JwtAuthFilter
 */
@Getter
@AllArgsConstructor
public class JwtPrincipal {
    /**
     * Unique identifier of the authenticated user.
     * Extracted from the 'sub' claim in the JWT.
     * Safe to use without database verification.
     */
    private final UUID userId;

    /**
     * Email address of the authenticated user.
     * Extracted from the JWT 'email' claim.
     * Useful for audit logging and user identification.
     */
    private final String email;

    /**
     * Granted authorities for this user (roles + permissions).
     * Extracted from the 'authorities' claim in the JWT.
     * Used by Spring Security for authorization checks.
     * Format: ["ROLE_ADMIN", "EXAM_VIEW", "ROOM_EDIT", ...]
     */
    private final Collection<? extends GrantedAuthority> authorities;
}

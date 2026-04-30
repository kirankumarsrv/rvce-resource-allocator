package com.rvce.scas.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Once-per-request filter that authenticates valid JWT bearer tokens.
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code tokenProvider}: validates the token signature and parses trusted claims.</li>
 * </ul>
 *
 * <p>Critical steps:</p>
 * <ol>
 *   <li>Read the Bearer token from the Authorization header only.</li>
 *   <li>Reject expired or malformed tokens without creating an authenticated principal.</li>
 *   <li>Convert verified JWT claims into a {@link JwtPrincipal}.</li>
 *   <li>Store the principal in the SecurityContext for downstream authorization checks.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    /**
        * Authenticates the request by reading the Bearer token from the Authorization header.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain downstream filter chain
     * @throws ServletException if filter processing fails
     * @throws IOException if request or response I/O fails
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token != null) {
            JwtTokenProvider.JwtValidationResult result = tokenProvider.validateAccessToken(token);

            if (result.isValid()) {
                setSecurityContext(result.getClaims(), request);
            } else if (result.isExpired()) {
                response.setHeader("X-Auth-Error", "TOKEN_EXPIRED");
            } else {
                log.warn("Invalid JWT from IP={} path={}", request.getRemoteAddr(), request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts a Bearer token from the request header.
     *
     * @param request current HTTP request
     * @return JWT string or {@code null}
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Populates the SecurityContext with the authenticated principal.
     *
     * @param claims verified JWT claims
     * @param request current HTTP request
     */
    private void setSecurityContext(Claims claims, HttpServletRequest request) {
        UUID userId = tokenProvider.extractUserId(claims);
        String email = tokenProvider.extractEmail(claims);
        List<String> roles = tokenProvider.extractRoles(claims);

        List<SimpleGrantedAuthority> authorities = roles == null
                ? List.of()
                : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

        JwtPrincipal principal = new JwtPrincipal(userId, email, authorities);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        authentication.setDetails(
                new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated user={} roles={}", email, roles);
    }

    /**
     * Skips the filter for public infrastructure endpoints.
     *
     * @param request current HTTP request
     * @return true when the filter should be bypassed
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/actuator/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}

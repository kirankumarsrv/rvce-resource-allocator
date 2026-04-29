package com.rvce.scas.security;

import com.rvce.scas.security.JwtTokenProvider.JwtValidationResult;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    /*
     * Beginner-level explanation of JwtAuthFilter responsibilities:
     *
     * - Purpose: Intercepts HTTP requests and attempts to authenticate them based on a
     *   Bearer access token present in the `Authorization` header. If authentication succeeds
     *   the filter creates an `Authentication` object and stores it in the `SecurityContext`.
     *
     * - OncePerRequestFilter: Ensures this filter runs once per HTTP request even when the
     *   request is forwarded internally. This avoids duplicate authentication logic.
     *
     * - Why we don't hit DB here: The JWT contains signed claims (userId, email, roles).
     *   Because the token is signed with our private key and verified with the public key,
     *   the filter trusts the claims and avoids a DB lookup on every request, which improves
     *   throughput and reduces DB load. Only login and refresh endpoints access DB.
     *
     * - Error handling: If the token is expired we set an `X-Auth-Error` header which the
     *   security entry point later converts into a JSON 401 response. Invalid tokens are
     *   logged but not propagated as exceptions to avoid stack traces on every bad request.
     *
     * - shouldNotFilter: Lists endpoints that must bypass this filter (login, refresh, health,
     *   API docs). Note: do not over-broaden permitAll in configuration to avoid exposing
     *   endpoints that should remain authenticated (e.g., logout).
     */
    @Override
    // T-005 DECISION [8]: OncePerRequestFilter ensures one security pass per request dispatch.
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // T-005 DECISION [10]: extract bearer token from Authorization header only.
        String token = extractTokenFromRequest(request);

        if (token != null) {
            JwtValidationResult result = tokenProvider.validateAccessToken(token);

            if (result.isValid()) {
                // T-005 DECISION [9]: no DB call on each request; trust signed claims.
                setSecurityContext(result.getClaims(), request);
            } else if (result.isExpired()) {
                // Header is consumed later by AuthenticationEntryPoint for structured JSON error code.
                response.setHeader("X-Auth-Error", "TOKEN_EXPIRED");
            } else {
                log.warn("Invalid JWT from IP={} path={}", request.getRemoteAddr(), request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        // Accept strict RFC6750 "Bearer <token>" form.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void setSecurityContext(Claims claims, HttpServletRequest request) {
        // Parse identity and authorities from verified claims.
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Allow login/refresh endpoints to run without access-token authentication.
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/actuator/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}

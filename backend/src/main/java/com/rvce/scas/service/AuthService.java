package com.rvce.scas.service;

import com.rvce.scas.dto.TokenPair;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.AccountLockedException;
import com.rvce.scas.exception.InvalidTokenException;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.security.JwtTokenProvider;
import com.rvce.scas.security.ScasPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15L;
    private static final String FAIL_COUNT_PREFIX = "login:fail:";
    private static final String LOCKOUT_PREFIX = "login:locked:";

    @Transactional
    public TokenPair login(String email, String rawPassword) {
        // Login flow step 2: fail fast if lockout key exists.
        // T-005 DECISION [15]: lockout state is Redis-based for fast distributed checks.
        checkLockout(email);

        try {
            // Login flow step 3: AuthenticationManager triggers UserDetailsService + password check.
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword)
            );

            ScasPrincipal principal = (ScasPrincipal) auth.getPrincipal();
            // Successful auth resets brute-force counter for this identity.
            clearFailCount(email);

            // Keep both ROLE_* and fine-grained permission authorities in JWT roles claim.
            List<String> roles = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Login flow step 5: mint short-lived RS256 access token.
            String accessToken = tokenProvider.generateAccessToken(
                    principal.getUserId(), principal.getEmail(), roles
            );
            // Login flow step 6: mint opaque revocable refresh token.
            String refreshToken = tokenProvider.generateRefreshToken(principal.getUserId());

            auditService.logLogin(principal.getUserId(), principal.getEmail(), true, null);
            log.info("Successful login for user: {}", email);

            return new TokenPair(accessToken, refreshToken);
        } catch (BadCredentialsException | LockedException e) {
            // Failed login increments Redis counter with TTL window.
            int failCount = incrementFailCount(email);

            if (failCount >= MAX_FAILED_ATTEMPTS) {
                lockAccount(email);
                auditService.logLogin(null, email, false, "ACCOUNT_LOCKED");
                throw new AccountLockedException(
                        "Account locked due to " + MAX_FAILED_ATTEMPTS
                                + " failed attempts. Try again after " + LOCKOUT_MINUTES + " minutes."
                );
            }

                // T-005 DECISION [16]: generic credential error message to avoid user enumeration via API.
            auditService.logLogin(null, email, false, "INVALID_CREDENTIALS");
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    @Transactional(readOnly = true)
    public TokenPair refresh(UUID userId, String refreshTokenId) {
        /*
         * Conceptual walkthrough of refresh flow (beginner notes):
         *
         * 1. Validate the opaque refresh token exists in Redis and belongs to the user.
         * 2. Load the fresh user record from the database to capture any changes to roles/permissions.
         *    This is important since refresh should issue tokens that reflect current privileges.
         * 3. Rotate the refresh token (delete old one, create a new one) to reduce replay windows.
         * 4. Issue a new signed JWT access token with updated claims.
         *
         * Notes & gotchas:
         * - Rotating by delete-then-create is conceptually correct but not atomic; a concurrent
         *   refresh could create races. Consider using Redis Lua scripts or a server-side lock
         *   for a fully atomic rotate in high-concurrency environments.
         * - Ensure the new access token contains the same level of authority detail (roles +
         *   fine-grained permissions) that your app relies on. Currently the implementation
         *   rebuilds only ROLE_* entries; if your app checks `hasAuthority("RESOURCE_ACTION")`
         *   you should include those permissions here as well.
         */
            // Refresh token must exist in Redis state and map to this user.
        tokenProvider.validateRefreshToken(userId, refreshTokenId)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired."));

            // T-005 DECISION [17]: reload latest user data from DB during refresh.
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidTokenException("User account is disabled."));

            // REVIEW-RISK (high): this currently includes only ROLE_* authorities.
            // Login token includes fine-grained permission authorities from ScasPrincipal.
            // After refresh, permission authorities (e.g., TIMETABLE_WRITE) may be lost.
            // This is a behavior mismatch versus DECISION [1] + [17] expectation of fresh complete claims.
        List<String> freshRoles = user.getUserRoles().stream()
                .map(ur -> "ROLE_" + ur.getRole().getName())
                .collect(Collectors.toList());

            // Rotate refresh token ID to limit replay from old stolen refresh token.
        String newRefreshToken = tokenProvider.rotateRefreshToken(userId, refreshTokenId);
        String newAccessToken = tokenProvider.generateAccessToken(userId, user.getEmail(), freshRoles);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken, UUID userId, String refreshTokenId) {
        // T-005 DECISION [6]: add access-token jti to blacklist and remove current refresh token.
        tokenProvider.logout(accessToken, userId, refreshTokenId);
        auditService.logLogout(userId);
    }

    public void logoutAllDevices(String accessToken, UUID userId) {
        // Blacklist current access token + delete all refresh tokens for user.
        tokenProvider.logout(accessToken, userId, null);
        tokenProvider.logoutAllDevices(userId);
        auditService.logLogout(userId);
    }

    private void checkLockout(String email) {
        // REVIEW-RISK (low): String#toLowerCase() should ideally use Locale.ROOT for deterministic behavior.
        String lockKey = LOCKOUT_PREFIX + email.toLowerCase();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
            throw new AccountLockedException("Account locked. Try again in " + ttl + " minute(s).");
        }
    }

    private int incrementFailCount(String email) {
        String failKey = FAIL_COUNT_PREFIX + email.toLowerCase();
        Long count = redisTemplate.opsForValue().increment(failKey);
        // First failure starts a sliding window TTL for brute-force tracking.
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }
        return count != null ? count.intValue() : 1;
    }

    private void lockAccount(String email) {
        String lockKey = LOCKOUT_PREFIX + email.toLowerCase();
        redisTemplate.opsForValue().set(lockKey, "1", LOCKOUT_MINUTES, TimeUnit.MINUTES);
        redisTemplate.delete(FAIL_COUNT_PREFIX + email.toLowerCase());
        log.warn("Account locked due to too many failed attempts: {}", email);
    }

    private void clearFailCount(String email) {
        redisTemplate.delete(FAIL_COUNT_PREFIX + email.toLowerCase());
    }
}

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

import java.util.*;
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
        checkLockout(email);

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword)
            );

            ScasPrincipal principal = (ScasPrincipal) auth.getPrincipal();
            clearFailCount(email);

            List<String> roles = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            String accessToken = tokenProvider.generateAccessToken(
                    principal.getUserId(), principal.getEmail(), roles
            );

            String refreshToken = tokenProvider.generateRefreshToken(principal.getUserId());

            auditService.logLogin(principal.getUserId(), principal.getEmail(), true, null);
            log.info("Successful login for user: {}", email);

            return new TokenPair(accessToken, refreshToken);

        } catch (BadCredentialsException | LockedException e) {
            int failCount = incrementFailCount(email);

            if (failCount >= MAX_FAILED_ATTEMPTS) {
                lockAccount(email);
                auditService.logLogin(null, email, false, "ACCOUNT_LOCKED");
                throw new AccountLockedException(
                        "Account locked due to " + MAX_FAILED_ATTEMPTS
                                + " failed attempts. Try again after " + LOCKOUT_MINUTES + " minutes."
                );
            }

            auditService.logLogin(null, email, false, "INVALID_CREDENTIALS");
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    @Transactional(readOnly = true)
    public TokenPair refresh(UUID userId, String refreshTokenId) {
        tokenProvider.validateRefreshToken(userId, refreshTokenId)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired."));

        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidTokenException("User account is disabled."));

        List<String> freshRoles = buildAuthoritiesForRefresh(user);

        String newRefreshToken = tokenProvider.rotateRefreshToken(userId, refreshTokenId);
        String newAccessToken = tokenProvider.generateAccessToken(userId, user.getEmail(), freshRoles);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    private List<String> buildAuthoritiesForRefresh(User user) {
        Set<String> authorities = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .flatMap(role -> {
                    Set<String> roleAuthorities = new HashSet<>();
                    roleAuthorities.add("ROLE_" + role.getName());
                    role.getRolePermissions().stream()
                            .map(rp -> rp.getPermission())
                            .map(perm -> perm.getResource().toUpperCase() + "_" + perm.getAction().toUpperCase())
                            .forEach(roleAuthorities::add);
                    return roleAuthorities.stream();
                })
                .collect(Collectors.toSet());

        return new ArrayList<>(authorities);
    }

    public void logout(String accessToken, UUID userId, String refreshTokenId) {
        tokenProvider.logout(accessToken, userId, refreshTokenId);
        auditService.logLogout(userId);
    }

    public void logoutAllDevices(String accessToken, UUID userId) {
        tokenProvider.logout(accessToken, userId, null);
        tokenProvider.logoutAllDevices(userId);
        auditService.logLogout(userId);
    }

    private void checkLockout(String email) {
        String lockKey = LOCKOUT_PREFIX + email.toLowerCase(Locale.ROOT);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
            throw new AccountLockedException("Account locked. Try again in " + ttl + " minute(s).");
        }
    }

    private int incrementFailCount(String email) {
        String failKey = FAIL_COUNT_PREFIX + email.toLowerCase(Locale.ROOT);
        Long count = redisTemplate.opsForValue().increment(failKey);

        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }

        return count != null ? count.intValue() : 1;
    }

    private void lockAccount(String email) {
        String lockKey = LOCKOUT_PREFIX + email.toLowerCase(Locale.ROOT);
        redisTemplate.opsForValue().set(lockKey, "1", LOCKOUT_MINUTES, TimeUnit.MINUTES);
        redisTemplate.delete(FAIL_COUNT_PREFIX + email.toLowerCase());
        log.warn("Account locked due to too many failed attempts: {}", email);
    }

    private void clearFailCount(String email) {
        redisTemplate.delete(FAIL_COUNT_PREFIX + email.toLowerCase(Locale.ROOT));
    }
}
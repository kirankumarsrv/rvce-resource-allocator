package com.rvce.scas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtTokenProvider {

    /*
     * Beginner-friendly overview and implementation notes for JwtTokenProvider:
     *
     * - Role: Responsible for creating and validating tokens used for stateless authentication.
     *   The provider issues two types of artifacts: short-lived RS256-signed JWT access tokens
     *   and opaque refresh-token IDs (UUIDs) stored in Redis.
     *
     * - RS256 vs HS256: RS256 uses an asymmetric keypair (private key signs, public key verifies).
     *   This allows verification by many services without sharing the private signing key.
     *   HS256 uses a shared secret, which is less safe in distributed systems.
     *
     * - Access token contents and claims:
     *     - `sub` (subject): userId (UUID string)
     *     - `email`: user's email
     *     - `roles`: list of authorities included at login time (ROLE_* and permission strings)
     *     - `jti`: unique token id used for blacklisting on logout
     *     - `exp` and `iat`: standard expiration and issued-at timestamps
     *
     * - jti (JWT ID): a short handle to blacklist a particular token prior to natural expiry.
     *   On logout we store the jti in Redis with TTL = remaining token lifetime. Every request
     *   checks Redis to reject blacklisted jtis.
     *
     * - Refresh tokens: opaque UUID values stored under `refresh:{userId}:{tokenId}` in Redis.
     *   Advantages: immediate revocation by deleting the key, and easy per-device tracking.
     *
     * - Key loading: private/public keys are loaded at bean construction time and kept in memory
     *   to avoid expensive KeyFactory operations on every request. In production you should
     *   provide stable key files (PEM) and avoid the fallback of generating ephemeral keys at
     *   startup (which invalidates tokens across restarts and between pods).
     *
     * - parseClaims: verifies signature using the public key and returns claims. Use the
     *   validated claims to extract identity and authorization details.
     *
     * - Redis usage patterns: `refresh:` prefix for refresh tokens and `blacklist:` prefix for
     *   jti blacklists. `logoutAllDevices` uses a key-pattern scan to delete all refresh tokens
     *   for a user (be careful with large keyspaces; consider Redis SCAN or a set index for scale).
     */

    // Cached keys are loaded once at bean creation time.
    // T-005 DECISION [7]: avoid key parsing/generation on request hot path.
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    // T-005 DECISION [4]: 15 minutes default for access-token TTL (900000 ms).
    private final long accessTokenExpiryMs;
    // T-005 DECISION [5]: 7 days default for refresh-token state in Redis.
    private final long refreshTokenExpirySeconds;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public JwtTokenProvider(
            @Value("${scas.jwt.private-key-path:}") String privateKeyPath,
            @Value("${scas.jwt.public-key-path:}") String publicKeyPath,
            @Value("${scas.jwt.access-token-expiry-ms:900000}") long accessTokenExpiryMs,
            @Value("${scas.jwt.refresh-token-expiry-seconds:604800}") long refreshTokenExpirySeconds,
            RedisTemplate<String, String> redisTemplate) throws Exception {

        // REVIEW-RISK (high): if key paths are blank, a new ephemeral key-pair is generated per app startup.
        // Consequence: all existing access tokens become invalid after restart; in multi-pod deployments,
        // pods may sign/verify with different keys, breaking cross-pod token validation.
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair pair = keyPairGenerator.generateKeyPair();

        // REVIEW-RISK (high): if only one path is configured (private or public), this can create mismatched
        // sign/verify keys because one key comes from file and the other from random generated pair.
        this.privateKey = privateKeyPath == null || privateKeyPath.isBlank()
                ? (PrivateKey) pair.getPrivate()
                : loadPrivateKey(privateKeyPath);
        this.publicKey = publicKeyPath == null || publicKeyPath.isBlank()
                ? (PublicKey) pair.getPublic()
                : loadPublicKey(publicKeyPath);

        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpiryMs);

        // RS256 signed JWT contains identity + authorization snapshot + revocation handle (jti).
        // T-005 DECISION [6]: jti enables blacklist-based logout before natural expiry.
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("type", "ACCESS")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        // T-005 DECISION [5]: refresh token is opaque random UUID, stored server-side in Redis.
        // This enables instant revocation by deleting key(s), unlike self-contained JWT refresh tokens.
        String tokenId = UUID.randomUUID().toString();
        String redisKey = REFRESH_PREFIX + userId + ":" + tokenId;
        String redisValue = userId.toString();

        redisTemplate.opsForValue().set(Objects.requireNonNull(redisKey), Objects.requireNonNull(redisValue));
        redisTemplate.expire(redisKey, refreshTokenExpirySeconds, TimeUnit.SECONDS);
        return tokenId;
    }

    public JwtValidationResult validateAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            // Reject tokens minted for other purposes.
            String tokenType = String.valueOf(claims.get("type"));
            if (!"ACCESS".equals(tokenType)) {
                return JwtValidationResult.invalid();
            }

            // T-005 DECISION [6]: revoked jti is denied until original token expiry.
            String jti = claims.getId();
            if (jti != null && isBlacklisted(jti)) {
                return JwtValidationResult.invalid();
            }

            return JwtValidationResult.valid(claims);
        } catch (ExpiredJwtException e) {
            return JwtValidationResult.expired();
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed");
            return JwtValidationResult.invalid();
        } catch (MalformedJwtException | IllegalArgumentException e) {
            return JwtValidationResult.invalid();
        } catch (JwtException e) {
            return JwtValidationResult.invalid();
        }
    }

    public Optional<UUID> validateRefreshToken(UUID userId, String tokenId) {
        // Key format encodes both user and token id, allowing per-device and per-user revocation patterns.
        String redisKey = REFRESH_PREFIX + userId + ":" + tokenId;
        String storedUserId = redisTemplate.opsForValue().get(redisKey);

        if (storedUserId == null) {
            return Optional.empty();
        }
        if (!storedUserId.equals(userId.toString())) {
            return Optional.empty();
        }
        return Optional.of(userId);
    }

    public String rotateRefreshToken(UUID userId, String oldTokenId) {
        // Rotate on refresh to reduce replay window.
        // REVIEW-RISK (medium): delete + generate is not atomic; concurrent refresh calls can both pass.
        String oldKey = REFRESH_PREFIX + userId + ":" + oldTokenId;
        redisTemplate.delete(oldKey);
        return generateRefreshToken(userId);
    }

    public void logout(String accessToken, UUID userId, String refreshTokenId) {
        try {
            Claims claims = parseClaims(accessToken);
            String jti = claims.getId();
            Date expiry = claims.getExpiration();
            if (jti != null && expiry != null) {
                // Blacklist entry expires exactly when token would have naturally expired.
                long ttlSeconds = Math.max(1L, (expiry.getTime() - System.currentTimeMillis()) / 1000L);
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Skipping access-token blacklist due to parse failure");
        }

        if (refreshTokenId != null && !refreshTokenId.isBlank()) {
            redisTemplate.delete(REFRESH_PREFIX + userId + ":" + refreshTokenId);
        }
    }

    public void logoutAllDevices(UUID userId) {
        // T-005 DECISION [5]: all refresh tokens for this user should be revoked here.
        // REVIEW-RISK (high): the current implementation uses redisTemplate.keys(pattern),
        // which performs a blocking Redis keyspace scan on the server thread.
        // The design comment says SCAN should be used, but the code below still uses KEYS.
        // For large Redis databases this can pause Redis and hurt all clients, not just auth.
        String pattern = REFRESH_PREFIX + userId + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return claims.get("roles", List.class);
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    private Claims parseClaims(String token) {
        // Verifies RS256 signature with public key and returns signed payload claims.
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        // Load PEM PKCS#8 private key used only by auth service for signing.
        String pem = Files.readString(Paths.get(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        // Load PEM X.509 public key used by any service for signature verification.
        String pem = Files.readString(Paths.get(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    @Getter
    public static class JwtValidationResult {
        private final boolean valid;
        private final boolean expired;
        private final Claims claims;

        private JwtValidationResult(boolean valid, boolean expired, Claims claims) {
            this.valid = valid;
            this.expired = expired;
            this.claims = claims;
        }

        public static JwtValidationResult valid(Claims claims) {
            return new JwtValidationResult(true, false, claims);
        }

        public static JwtValidationResult expired() {
            return new JwtValidationResult(false, true, null);
        }

        public static JwtValidationResult invalid() {
            return new JwtValidationResult(false, false, null);
        }
    }
}

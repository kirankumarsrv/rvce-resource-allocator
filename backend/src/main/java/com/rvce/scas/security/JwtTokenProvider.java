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
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * Central JWT and refresh-token service for the authentication flow.
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code privateKey}: RSA private key used to sign access tokens.</li>
 *   <li>{@code publicKey}: RSA public key used to verify access tokens.</li>
 *   <li>{@code accessTokenExpiryMs}: lifetime for signed access tokens.</li>
 *   <li>{@code refreshTokenExpirySeconds}: lifetime for Redis-backed refresh tokens.</li>
 *   <li>{@code redisTemplate}: Redis access for refresh tokens and blacklist entries.</li>
 * </ul>
 *
 * <p>Critical steps:</p>
 * <ol>
 *   <li>Load or generate the RSA key pair once at startup.</li>
 *   <li>Sign access tokens with RS256 and include a {@code jti} for blacklist support.</li>
 *   <li>Store opaque refresh tokens in Redis so logout can revoke them immediately.</li>
 *   <li>Check Redis for blacklisted JTIs before accepting an access token.</li>
 * </ol>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    // T-005 DECISION [7]: avoid key parsing/generation on request hot path.
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpirySeconds;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Loads the RSA key pair and configures Redis-backed token storage.
     *
     * @param privateKeyPath path to the private key file or blank for an ephemeral pair
     * @param publicKeyPath path to the public key file or blank for an ephemeral pair
     * @param accessTokenExpiryMs access-token lifetime in milliseconds
     * @param refreshTokenExpirySeconds refresh-token lifetime in seconds
     * @param redisTemplate Redis template used for refresh tokens and blacklist data
     * @throws Exception if key loading fails
     */
    public JwtTokenProvider(
            @Value("${scas.jwt.private-key-path:}") String privateKeyPath,
            @Value("${scas.jwt.public-key-path:}") String publicKeyPath,
            @Value("${scas.jwt.access-token-expiry-ms:900000}") long accessTokenExpiryMs,
            @Value("${scas.jwt.refresh-token-expiry-seconds:604800}") long refreshTokenExpirySeconds,
            RedisTemplate<String, String> redisTemplate) throws Exception {

        // FIX: validate key configuration to prevent ephemeral key fallback.
        // If both paths are blank, a new keypair is generated per startup,
        // invalidating all tokens across restarts and breaking multi-pod deployments.
        if ((privateKeyPath == null || privateKeyPath.isBlank()) &&
            (publicKeyPath == null || publicKeyPath.isBlank())) {
            log.warn("SECURITY WARNING: both private-key-path and public-key-path are blank. "
                    + "Generating ephemeral RSA keypair. This will invalidate tokens after restart. "
                    + "For production, configure scas.jwt.private-key-path and scas.jwt.public-key-path.");
        }
        // FIX: ensure both keys come from the same source (both files or both generated).
        // Mixing one file key and one generated key creates sign/verify mismatch.
        boolean bothFilesPassed = (privateKeyPath != null && !privateKeyPath.isBlank()) &&
                                 (publicKeyPath != null && !publicKeyPath.isBlank());
        boolean bothBlank = (privateKeyPath == null || privateKeyPath.isBlank()) &&
                           (publicKeyPath == null || publicKeyPath.isBlank());
        if (!(bothFilesPassed || bothBlank)) {
            throw new IllegalArgumentException(
                    "Both private-key-path and public-key-path must be configured, or both must be blank. "
                    + "Partial configuration creates key mismatch.");
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair pair = keyPairGenerator.generateKeyPair();
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

    /**
     * Generates a signed access token for the authenticated user.
     *
     * @param userId authenticated user id
     * @param email authenticated email address
     * @param roles granted authorities to embed in the token
     * @return compact RS256 JWT
     */
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

    /**
     * Generates an opaque refresh token and stores it in Redis.
     *
     * @param userId owner of the refresh token
     * @return opaque refresh-token id
     */
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

    /**
     * Validates an access token and returns a structured result.
     *
     * @param token compact JWT string
     * @return validation state with claims when valid
     */
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

    /**
     * Validates that a refresh token exists for the supplied user.
     *
     * @param userId user identifier
     * @param tokenId opaque refresh-token id
     * @return user id when the refresh token is valid
     */
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

    /**
     * Rotates the refresh token by deleting the old token and issuing a new one.
     *
     * @param userId user identifier
     * @param oldTokenId refresh-token id to invalidate
     * @return new opaque refresh-token id
     */
    public String rotateRefreshToken(UUID userId, String oldTokenId) {
        // Rotate on refresh to reduce replay window.
        // REVIEW-RISK (medium): delete + generate is not atomic; concurrent refresh calls can both pass.
        String oldKey = REFRESH_PREFIX + userId + ":" + oldTokenId;
        redisTemplate.delete(oldKey);
        return generateRefreshToken(userId);
    }

    /**
     * Blacklists the current access token and removes the current refresh token when present.
     *
     * @param accessToken bearer access token to blacklist
     * @param userId user identifier
     * @param refreshTokenId current refresh-token id, if any
     */
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

    /**
     * Revokes every refresh token issued for the supplied user.
     *
     * @param userId user identifier
     */
    public void logoutAllDevices(UUID userId) {
        // T-005 DECISION [5]: all refresh tokens for this user should be revoked here.
        // FIX: use cursor-based SCAN iteration to avoid blocking Redis entirely.
        // KEYS() blocks the Redis server; SCAN iterates with non-blocking cursor.
        String pattern = REFRESH_PREFIX + userId + ":*";
        try {
            Set<String> keys = new HashSet<>();
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();
            try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to logout all devices for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Extracts the user id claim from a verified JWT.
     *
     * @param claims verified claims
     * @return parsed user id
     */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the email claim from a verified JWT.
     *
     * @param claims verified claims
     * @return email from the token
     */
    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    /**
     * Extracts the authority list from a verified JWT.
     *
     * @param claims verified claims
     * @return token authorities, or an empty list when absent
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return claims.get("roles", List.class);
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

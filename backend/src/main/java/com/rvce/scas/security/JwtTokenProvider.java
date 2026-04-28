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

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpiryMs;
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

    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpiryMs);

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
            String tokenType = String.valueOf(claims.get("type"));
            if (!"ACCESS".equals(tokenType)) {
                return JwtValidationResult.invalid();
            }

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
        String pem = Files.readString(Paths.get(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey loadPublicKey(String path) throws Exception {
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

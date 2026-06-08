package com.rvce.scas.security;

import com.rvce.scas.config.EncryptedStringConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.KeyGenerator;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncryptionAndJwtCryptoTest {

    @BeforeEach
    void resetConverter() {
        EncryptedStringConverter.setEncryptionUtil(null);
    }

    @Test
    void aesEncryptionRoundTripsAndConverterUsesIt() throws Exception {
        EncryptionUtil encryptionUtil = new EncryptionUtil(generateAesKeyBase64());
        String plaintext = "student@example.com";

        String ciphertext = encryptionUtil.encrypt(plaintext);

        assertNotEquals(plaintext, ciphertext);
        assertEquals(plaintext, encryptionUtil.decrypt(ciphertext));

        EncryptedStringConverter.setEncryptionUtil(encryptionUtil);
        EncryptedStringConverter converter = new EncryptedStringConverter();

        String dbValue = converter.convertToDatabaseColumn(plaintext);
        String entityValue = converter.convertToEntityAttribute(dbValue);

        assertNotEquals(plaintext, dbValue);
        assertEquals(plaintext, entityValue);
    }

    @Test
    void rsaJwtSigningAndValidationRoundTripsLocally() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        RedisTemplate<String, String> redisTemplate = mockRedisTemplate();

        JwtTokenProvider provider = new JwtTokenProvider(
            "",
            "",
                pem(keyPair.getPrivate(), "PRIVATE KEY"),
                pem(keyPair.getPublic(), "PUBLIC KEY"),
            60_000L,
            3_600L,
                redisTemplate);

        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "student@rvce.edu.in", List.of("ROLE_STUDENT"));
        JwtTokenProvider.JwtValidationResult validationResult = provider.validateAccessToken(token);

        assertTrue(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(userId.toString(), provider.extractUserId(validationResult.getClaims()).toString());
        assertEquals("student@rvce.edu.in", provider.extractEmail(validationResult.getClaims()));
        assertEquals(List.of("ROLE_STUDENT"), provider.extractRoles(validationResult.getClaims()));
    }

    private String generateAesKeyBase64() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256, SecureRandom.getInstanceStrong());
        return Base64.getEncoder().encodeToString(generator.generateKey().getEncoded());
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String pem(PrivateKey key, String label) {
        return pem(key.getEncoded(), label);
    }

    private String pem(PublicKey key, String label) {
        return pem(key.getEncoded(), label);
    }

    private String pem(byte[] encoded, String label) {
        String base64 = Base64.getEncoder().encodeToString(encoded);
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN ").append(label).append("-----\n");
        for (int index = 0; index < base64.length(); index += 64) {
            builder.append(base64, index, Math.min(index + 64, base64.length())).append('\n');
        }
        builder.append("-----END ").append(label).append("-----\n");
        return builder.toString();
    }

    private RedisTemplate<String, String> mockRedisTemplate() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.delete(anyString())).thenReturn(Boolean.TRUE);
        return redisTemplate;
    }
}

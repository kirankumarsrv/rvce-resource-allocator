package com.rvce.scas.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * AES-256-GCM encryption utility used for transparent JPA column encryption.
 */
@Slf4j
public class EncryptionUtil {

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV
    private static final int GCM_TAG_LENGTH = 128; // 128-bit tag
    private static final int AES_KEY_SIZE = 256;
    private static final Pattern BASE64_PATTERN = Pattern.compile("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
    private static final int MIN_BASE64_LENGTH = 24;

    private final SecretKey key;

    public EncryptionUtil(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("No scas.encryption.key configured; generating temporary AES key. " +
                    "This is insecure for production.");
            this.key = generateKey();
        } else {
            this.key = loadKey(base64Key);
        }
        log.info("EncryptionUtil initialized{}", key != null ? "" : " with no key");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Failed to encrypt value", e);
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return encryptedBase64;
        }

        boolean looksLikeBase64 = encryptedBase64.length() >= MIN_BASE64_LENGTH
                && encryptedBase64.length() % 4 == 0
                && BASE64_PATTERN.matcher(encryptedBase64).matches();
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            if (combined.length < GCM_IV_LENGTH + 1) {
                if (looksLikeBase64) {
                    log.error("Encrypted value looks like base64 but is shorter than a valid AES-GCM payload.");
                    throw new IllegalStateException("Decryption failed: invalid encrypted payload");
                }
                return encryptedBase64;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            byte[] ciphertext = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return new String(cipher.doFinal(ciphertext));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            if (looksLikeBase64) {
                log.error("Failed to decrypt base64-like value. This may indicate a bad encryption key or corrupted encrypted data.", e);
                throw new IllegalStateException("Decryption failed for base64-like value", e);
            }
            log.debug("Value is not encrypted data; returning raw plaintext.", e);
            return encryptedBase64;
        } catch (Exception e) {
            if (looksLikeBase64) {
                log.error("Failed to decrypt base64-like value. This may indicate a bad encryption key or corrupted encrypted data.", e);
                throw new IllegalStateException("Decryption failed for base64-like value", e);
            }
            log.debug("Value is not encrypted data; returning raw plaintext.", e);
            return encryptedBase64;
        }
    }

    private SecretKey loadKey(String base64Key) {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        if (bytes.length != AES_KEY_SIZE / Byte.SIZE) {
            throw new IllegalArgumentException("Expected 256-bit key, but found " + bytes.length * Byte.SIZE + " bits");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private SecretKey generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(AES_KEY_SIZE, SecureRandom.getInstanceStrong());
            return generator.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate AES key", e);
        }
    }
}

# T-602 Implementation Code Templates

This file contains ready-to-use code templates for implementing the T-602 security requirements.

---

## 1. AES-256 Encryption Utility

### File: `backend/src/main/java/com/rvce/scas/security/EncryptionUtil.java`

```java
package com.rvce.scas.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for column-level encryption.
 * 
 * Features:
 * - Uses AES-256 in GCM mode for authenticated encryption
 * - IV (initialization vector) is randomly generated for each encryption
 * - Authentication tag provides integrity verification
 * - Transparent encryption/decryption for JPA
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV (standard for GCM)
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag
    private static final int AES_KEY_SIZE = 256; // 256-bit key

    private final SecretKey encryptionKey;

    public EncryptionUtil(@Value("${scas.encryption.key:}") String base64EncodedKey) {
        if (base64EncodedKey == null || base64EncodedKey.isBlank()) {
            log.warn("SECURITY WARNING: Encryption key not provided. Generating temporary key. " +
                    "For production, set scas.encryption.key in AWS Secrets Manager.");
            this.encryptionKey = generateNewKey();
        } else {
            this.encryptionKey = loadKeyFromBase64(base64EncodedKey);
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * 
     * Encrypted format: [IV(12 bytes)][Ciphertext][AuthTag(16 bytes)]
     * 
     * @param plaintext data to encrypt
     * @return base64-encoded encrypted data
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext; // Don't encrypt empty/null values
        }

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            
            // Generate random IV for this encryption
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);
            
            // Encrypt the plaintext
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
            
            // Combine IV + Ciphertext for storage
            byte[] encryptedWithIv = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, encryptedWithIv, iv.length, encryptedBytes.length);
            
            // Return as base64 for database storage
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption error", e);
        }
    }

    /**
     * Decrypts base64-encoded encrypted data using AES-256-GCM.
     * 
     * @param encryptedBase64 base64-encoded encrypted data
     * @return plaintext
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return encryptedBase64; // Don't decrypt empty/null values
        }

        try {
            // Decode from base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedBase64);
            
            // Extract IV (first 12 bytes)
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            
            // Extract ciphertext (remaining bytes)
            byte[] ciphertext = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);
            
            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return new String(decryptedBytes);
            
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption error", e);
        }
    }

    /**
     * Generates a new 256-bit AES key.
     * Used only for ephemeral keys; production uses key from Secrets Manager.
     */
    private SecretKey generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Key generation error", e);
        }
    }

    /**
     * Loads AES key from base64-encoded string.
     * Expected format: base64-encoded 32 bytes (256 bits)
     */
    private SecretKey loadKeyFromBase64(String base64Key) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            if (decodedKey.length != 32) {
                throw new IllegalArgumentException(
                    "Invalid key size: " + decodedKey.length + " bytes. Expected 32 bytes (256 bits).");
            }
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } catch (IllegalArgumentException e) {
            log.error("Invalid encryption key format", e);
            throw e;
        }
    }
}
```

---

## 2. JPA AttributeConverter for String Encryption

### File: `backend/src/main/java/com/rvce/scas/config/EncryptedStringConverter.java`

```java
package com.rvce.scas.config;

import com.rvce.scas.security.EncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts String columns.
 * 
 * Usage:
 *   @Convert(converter = EncryptedStringConverter.class)
 *   @Column(name = "email")
 *   private String email;
 * 
 * This converter:
 * - Encrypts plaintext to base64 before persisting to database
 * - Decrypts base64 to plaintext when loading from database
 * - Handles null/empty values gracefully
 */
@Component
@Converter(autoApply = false) // Don't auto-apply; use @Convert annotation
@AllArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;

    /**
     * Called before persisting to database.
     * Converts plaintext to encrypted base64 string.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute; // Don't encrypt null/empty
        }
        return encryptionUtil.encrypt(attribute);
    }

    /**
     * Called when loading from database.
     * Converts encrypted base64 string to plaintext.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData; // Don't decrypt null/empty
        }
        return encryptionUtil.decrypt(dbData);
    }
}
```

---

## 3. Update User Entity

### File: `backend/src/main/java/com/rvce/scas/entity/User.java` (modifications)

```java
package com.rvce.scas.entity;

import com.rvce.scas.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
// ... other imports ...

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * ENCRYPTED: Email is sensitive PII and login credential.
     * Stored as encrypted ciphertext in database.
     * Column length increased to 500 to accommodate encrypted data.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email", nullable = false, length = 500)
    private String email;

    /**
     * ENCRYPTED: University Serial Number is sensitive PII.
     * Stored as encrypted ciphertext in database.
     * Column length increased to 100 to accommodate encrypted data.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "usn", length = 100)
    private String usn;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // ... rest of entity ...
}
```

---

## 4. Update ExamStudent Entity

### File: `backend/src/main/java/com/rvce/scas/entity/ExamStudent.java` (modifications)

```java
package com.rvce.scas.entity;

import com.rvce.scas.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
// ... other imports ...

@Getter
@Setter
@Entity
@Table(
    name = "exam_students",
    uniqueConstraints = @UniqueConstraint(name = "uq_exam_student", columnNames = {"exam_id", "usn"})
)
public class ExamStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", nullable = false, updatable = false)
    private UUID entryId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_id")
    private UUID studentId;

    /**
     * ENCRYPTED: University Serial Number is sensitive PII.
     * Stored as encrypted ciphertext in database.
     * Column length increased to 100 to accommodate encrypted data.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "usn", nullable = false, length = 100)
    private String usn;

    @Column(name = "student_name", nullable = false, length = 150)
    private String studentName;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    // ... rest of entity ...
}
```

---

## 5. Flyway Database Migration

### File: `backend/src/main/resources/db/migration/V6__enable_column_encryption.sql`

```sql
-- T-602: Extend column lengths for encrypted data
-- Encrypted data is larger than plaintext due to IV and authentication tag
-- 
-- Example:
-- Original:    "student@example.com" (19 characters)
-- Encrypted:   "A7F3B2C9D1E4F5A6B7C8D9E0F1A2B3C4..." (~80+ characters in base64)

-- Extend users table columns
ALTER TABLE users
ALTER COLUMN email TYPE VARCHAR(500),
ALTER COLUMN usn TYPE VARCHAR(100);

-- Extend exam_students table column
ALTER TABLE exam_students
ALTER COLUMN usn TYPE VARCHAR(100);

-- Add index for encrypted USN searches (if needed for performance)
CREATE INDEX idx_exam_students_usn ON exam_students(usn);
```

---

## 6. Public Key Endpoint Controller

### File: `backend/src/main/java/com/rvce/scas/controller/PublicKeyController.java`

```java
package com.rvce.scas.controller;

import com.rvce.scas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes JWT public keys for service-to-service token validation.
 * 
 * Endpoints:
 * - GET /public-keys/jwt.pub       : PEM-encoded public key (single key)
 * - GET /public-keys/jwks.json     : JWKS format with multiple keys
 * 
 * Usage by other services:
 * 1. Fetch public key endpoint
 * 2. Verify JWT signature using public key
 * 3. Validate token claims (exp, nbf, etc.)
 */
@Slf4j
@RestController
@RequestMapping("/api/public-keys")
@CrossOrigin(origins = "*") // Needed for browser-based clients
@RequiredArgsConstructor
public class PublicKeyController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Returns the current JWT public key in PEM format.
     * 
     * @return PEM-encoded RSA public key
     */
    @GetMapping("/jwt.pub")
    public ResponseEntity<String> getJwtPublicKey() {
        try {
            String publicKeyPem = jwtTokenProvider.getPublicKeyPem();
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(publicKeyPem);
        } catch (Exception e) {
            log.error("Failed to retrieve public key", e);
            return ResponseEntity.status(500).body("Public key unavailable");
        }
    }

    /**
     * Returns JWT public keys in JWKS (JSON Web Key Set) format.
     * 
     * JWKS format allows clients to discover and cache multiple keys,
     * supporting key rotation without re-fetching single keys.
     * 
     * @return JWKS response with current and recent keys
     */
    @GetMapping("/jwks.json")
    public ResponseEntity<JwksResponse> getJwks() {
        try {
            JwksResponse jwks = jwtTokenProvider.getJwks();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jwks);
        } catch (Exception e) {
            log.error("Failed to retrieve JWKS", e);
            return ResponseEntity.status(500).build();
        }
    }
}
```

---

## 7. JWKS Response DTO

### File: `backend/src/main/java/com/rvce/scas/dto/JwksResponse.java`

```java
package com.rvce.scas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JWKS (JSON Web Key Set) response format.
 * 
 * Standardized format for publishing public keys.
 * Used by OAuth2/OIDC libraries to discover and validate keys.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwksResponse {
    private List<JwtKey> keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JwtKey {
        /**
         * Key ID (optional, but recommended for key rotation).
         * Example: "2024-Q1", "2024-Q2", etc.
         */
        private String kid;

        /**
         * Key type: "RSA" for RSA keys
         */
        private String kty;

        /**
         * Algorithm: "RS256" for RSA with SHA-256
         */
        private String alg;

        /**
         * Key use: "sig" for signing, "enc" for encryption
         */
        @JsonProperty("use")
        private String keyUse;

        /**
         * RSA modulus (n) in base64url format
         */
        private String n;

        /**
         * RSA public exponent (e) in base64url format
         */
        private String e;
    }
}
```

---

## 8. Database SSL Configuration

### Update: `backend/src/main/resources/application.yaml`

```yaml
spring:
  # ... other config ...

---

# DEVELOPMENT PROFILE
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    driver-class-name: org.postgresql.Driver
    # Local PostgreSQL: no SSL required for development
    url: jdbc:postgresql://localhost:5432/scas_db?stringtype=unspecified&sslmode=disable
    username: ${DB_USER:scas}
    password: ${DB_PASSWORD:scas_dev_password}

---

# PRODUCTION PROFILE
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    driver-class-name: org.postgresql.Driver
    # AWS RDS: SSL/TLS required
    # sslmode=require: Connection MUST use SSL, fails if unavailable
    # sslrootcert: Path to RDS CA certificate for verification
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:scas_db}?sslmode=require&sslrootcert=/opt/certs/rds-ca.pem
    username: ${DB_USER:scas}
    password: ${DB_PASSWORD:}  # MUST be provided, no default
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      max-lifetime: 1800000
      connection-timeout: 30000
      validation-timeout: 5000
      idle-timeout: 600000
```

---

## 9. AWS Secrets Manager Configuration

### File: `backend/src/main/resources/application-secrets.yml`

```yaml
# Spring Cloud AWS Secrets Manager integration
# Automatically loads secrets from AWS Secrets Manager at startup

spring:
  config:
    import:
      # Load secrets from AWS Secrets Manager
      # Expects a secret named: prod/scas/secrets (for production)
      # Or dev/scas/secrets (for development)
      - aws-secrets:${SCAS_SECRETS_NAME:dev/scas/secrets}

aws:
  secretsmanager:
    # AWS region where secrets are stored
    region: ${AWS_REGION:us-east-1}
    # Optional: custom endpoint (useful for LocalStack in development)
    endpoint: ${AWS_SECRETSMANAGER_ENDPOINT:}

# Application-specific secrets (loaded from AWS Secrets Manager)
scas:
  encryption:
    # 256-bit AES key in base64 format
    # Loaded from AWS Secrets Manager key: "encryption-key"
    key: ${encryption-key}
  
  jwt:
    # 2048-bit RSA private key in PEM format
    # Loaded from AWS Secrets Manager key: "jwt-private-key"
    private-key: ${jwt-private-key}
    
    # 2048-bit RSA public key in PEM format
    # Loaded from AWS Secrets Manager key: "jwt-public-key"
    public-key: ${jwt-public-key}
    
    access-token-expiry-ms: ${JWT_ACCESS_TOKEN_EXPIRY_MS:900000}  # 15 minutes
    refresh-token-expiry-seconds: ${JWT_REFRESH_TOKEN_EXPIRY_SECONDS:604800}  # 7 days

spring:
  datasource:
    # Database password from AWS Secrets Manager
    password: ${db-password}
  
  mail:
    # SMTP credentials from AWS Secrets Manager
    username: ${email-username}
    password: ${email-password}
```

---

## 10. Gradle Dependencies

### Update: `backend/build.gradle.kts`

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // AWS SDK for Secrets Manager
    implementation("software.amazon.awssdk:secretsmanager:2.25.0")
    
    // Spring Cloud AWS integration for Secrets Manager
    implementation("io.awspring.cloud:spring-cloud-aws-secrets-manager-config:2.4.4")
    
    // For RSA key operations in JwtTokenProvider
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    
    // Existing dependencies...
}
```

---

## 11. Key Rotation Scheduler

### File: `backend/src/main/java/com/rvce/scas/service/KeyRotationScheduler.java`

```java
package com.rvce.scas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;

/**
 * Scheduled key rotation for JWT keys.
 * 
 * Rotation Schedule: First day of every month at 00:00 UTC
 * This ensures quarterly rotation (approximately every 3 months).
 * 
 * Process:
 * 1. Generate new 2048-bit RSA key pair
 * 2. Store new private key in AWS Secrets Manager
 * 3. Update public key endpoint
 * 4. Maintain overlap period (48 hours) where both keys are valid
 * 5. Gradually phase out old key
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyRotationScheduler {

    private final SecretManagerService secretManagerService;
    private final KeyRotationMetadataService keyRotationMetadataService;

    /**
     * Scheduled task: Generate and rotate keys quarterly (first day of month).
     * 
     * Cron: "0 0 0 1 * *" = 00:00:00 on the 1st of every month
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void rotateKeys() {
        try {
            log.info("Starting quarterly JWT key rotation...");

            // Step 1: Generate new RSA key pair (2048-bit)
            KeyPair newKeyPair = generateNewRsaKeyPair();
            
            // Step 2: Create rotation metadata
            String newKeyId = "KEY_" + System.currentTimeMillis();
            KeyRotationMetadata metadata = new KeyRotationMetadata();
            metadata.setKeyId(newKeyId);
            metadata.setActivationTime(Instant.now());
            metadata.setDeprecationTime(Instant.now().plusSeconds(48 * 3600)); // 48 hours
            metadata.setIsActive(true);
            
            // Step 3: Store in AWS Secrets Manager
            secretManagerService.storeKeyPair(newKeyId, newKeyPair, metadata);
            
            // Step 4: Update rotation metadata
            keyRotationMetadataService.save(metadata);
            
            // Step 5: Mark previous key as deprecated
            keyRotationMetadataService.deprecatePreviousKeys(newKeyId);
            
            log.info("Key rotation completed successfully. New key ID: {}", newKeyId);

        } catch (Exception e) {
            log.error("CRITICAL: Key rotation failed", e);
            // Alert ops team immediately
            sendAlert("Key rotation failed: " + e.getMessage());
        }
    }

    /**
     * Generates a new 2048-bit RSA key pair.
     * 
     * @return new KeyPair
     * @throws Exception if key generation fails
     */
    private KeyPair generateNewRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private void sendAlert(String message) {
        // TODO: Send Slack/PagerDuty alert
        log.error("ALERT: {}", message);
    }
}
```

---

## 12. Key Rotation Metadata Entity

### File: `backend/src/main/java/com/rvce/scas/entity/KeyRotationMetadata.java`

```java
package com.rvce.scas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks JWT key rotation history and current key status.
 * 
 * Allows applications to:
 * - Know which key is currently active for signing
 * - Support multiple keys during overlap period
 * - Audit key rotation history
 */
@Getter
@Setter
@Entity
@Table(name = "key_rotation_metadata")
public class KeyRotationMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Unique identifier for this key pair.
     * Format: "KEY_" + timestamp
     * Example: "KEY_1704067200000"
     */
    @Column(name = "key_id", nullable = false, unique = true)
    private String keyId;

    /**
     * When this key was activated for signing new tokens.
     */
    @Column(name = "activation_time", nullable = false)
    private Instant activationTime;

    /**
     * When this key should be phased out.
     * During [activationTime, deprecationTime], both old and new keys are valid.
     * After deprecationTime, only new key is used.
     */
    @Column(name = "deprecation_time")
    private Instant deprecationTime;

    /**
     * Whether this key is currently used for signing new tokens.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /**
     * Algorithm: "RS256" (RSA with SHA-256)
     */
    @Column(name = "algorithm")
    private String algorithm;

    /**
     * When this key was rotated out completely.
     * No new tokens signed, old tokens might still be valid until natural expiry.
     */
    @Column(name = "retired_time")
    private Instant retiredTime;
}
```

---

## 13. Unit Test for Encryption

### File: `backend/src/test/java/com/rvce/scas/security/EncryptionUtilTest.java`

```java
package com.rvce.scas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Encryption Utility Tests")
class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        // Initialize with temporary key (generated in constructor)
        encryptionUtil = new EncryptionUtil("");
    }

    @Test
    @DisplayName("Should encrypt and decrypt plaintext correctly")
    void testEncryptDecryptRoundTrip() {
        String plaintext = "student@example.com";
        
        // Encrypt
        String encrypted = encryptionUtil.encrypt(plaintext);
        
        // Verify encrypted text is different
        assertNotEquals(plaintext, encrypted);
        
        // Verify encrypted text is non-empty
        assertFalse(encrypted.isEmpty());
        
        // Decrypt
        String decrypted = encryptionUtil.decrypt(encrypted);
        
        // Verify decrypted matches original
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Should handle multiple encryptions of same plaintext differently")
    void testEncryptionVariation() {
        String plaintext = "student@example.com";
        
        String encrypted1 = encryptionUtil.encrypt(plaintext);
        String encrypted2 = encryptionUtil.encrypt(plaintext);
        
        // Each encryption should produce different ciphertext (different IV)
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionUtil.decrypt(encrypted1));
        assertEquals(plaintext, encryptionUtil.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullHandling() {
        assertNull(encryptionUtil.encrypt(null));
        assertNull(encryptionUtil.decrypt(null));
    }

    @Test
    @DisplayName("Should handle empty strings gracefully")
    void testEmptyStringHandling() {
        assertEquals("", encryptionUtil.encrypt(""));
        assertEquals("", encryptionUtil.decrypt(""));
    }

    @Test
    @DisplayName("Should encrypt various data types")
    void testVariousDataTypes() {
        String[] testData = {
            "user@example.com",
            "1234567890",  // Phone number
            "CSE21001",    // USN
            "Dr. John Doe",  // Name
            "John.Doe@university.edu",  // Email
        };
        
        for (String data : testData) {
            String encrypted = encryptionUtil.encrypt(data);
            String decrypted = encryptionUtil.decrypt(encrypted);
            assertEquals(data, decrypted, "Failed for: " + data);
        }
    }

    @Test
    @DisplayName("Should fail gracefully on corrupted ciphertext")
    void testCorruptedCiphertext() {
        String plaintext = "student@example.com";
        String encrypted = encryptionUtil.encrypt(plaintext);
        
        // Corrupt the encrypted data
        String corrupted = encrypted.substring(0, encrypted.length() - 5);
        
        assertThrows(RuntimeException.class, () -> {
            encryptionUtil.decrypt(corrupted);
        });
    }
}
```

---

## 14. Docker Image with RDS Certificate

### Update: `backend/Dockerfile`

```dockerfile
FROM openjdk:17-slim

# Download RDS CA certificate for SSL connections
RUN mkdir -p /opt/certs && \
    apt-get update && \
    apt-get install -y curl && \
    curl https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
      --output /opt/certs/rds-ca.pem && \
    apt-get remove -y curl && \
    apt-get clean

COPY build/libs/scas-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 15. Kubernetes ExternalSecrets Configuration

### File: `k8s/external-secrets.yaml`

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: scas-app-sa
  namespace: default
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT_ID:role/scas-app-role

---

# SecretStore: Defines connection to AWS Secrets Manager
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-store
  namespace: default
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: scas-app-sa

---

# ExternalSecret: Syncs AWS secret to Kubernetes Secret
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: scas-secrets
  namespace: default
spec:
  refreshInterval: 1h  # Refresh from AWS every hour
  
  secretStoreRef:
    name: aws-secrets-store
    kind: SecretStore
  
  target:
    name: scas-secrets-k8s  # Name of K8s Secret to create
    creationPolicy: Owner
    template:
      type: Opaque
      data:
        # Plaintext templating from AWS secret
        DB_PASSWORD: "{{ .db_password }}"
        ENCRYPTION_KEY: "{{ .encryption_key }}"
        JWT_PRIVATE_KEY: "{{ .jwt_private_key }}"
        JWT_PUBLIC_KEY: "{{ .jwt_public_key }}"
  
  # Map AWS Secrets Manager keys to K8s Secret keys
  data:
    - secretKey: db_password
      remoteRef:
        key: prod/scas/secrets
        property: db-password
    
    - secretKey: encryption_key
      remoteRef:
        key: prod/scas/secrets
        property: encryption-key
    
    - secretKey: jwt_private_key
      remoteRef:
        key: prod/scas/secrets
        property: jwt-private-key
    
    - secretKey: jwt_public_key
      remoteRef:
        key: prod/scas/secrets
        property: jwt-public-key

---

# Pod: Consumes K8s Secret as environment variables
apiVersion: v1
kind: Pod
metadata:
  name: scas-app
spec:
  serviceAccountName: scas-app-sa
  
  containers:
  - name: scas
    image: scas-app:latest
    imagePullPolicy: Always
    
    ports:
    - containerPort: 8080
      name: http
    
    env:
    # Spring profile
    - name: SPRING_PROFILES_ACTIVE
      value: prod
    
    # Database connection (secret)
    - name: DB_URL
      value: "jdbc:postgresql://prod-rds.xxx.us-east-1.rds.amazonaws.com:5432/scas_db?sslmode=require"
    - name: DB_USER
      value: "scas"
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: scas-secrets-k8s
          key: DB_PASSWORD
    
    # Encryption key (secret)
    - name: SCAS_ENCRYPTION_KEY
      valueFrom:
        secretKeyRef:
          name: scas-secrets-k8s
          key: ENCRYPTION_KEY
    
    # JWT keys (secret)
    - name: JWT_PRIVATE_KEY
      valueFrom:
        secretKeyRef:
          name: scas-secrets-k8s
          key: JWT_PRIVATE_KEY
    - name: JWT_PUBLIC_KEY
      valueFrom:
        secretKeyRef:
          name: scas-secrets-k8s
          key: JWT_PUBLIC_KEY
    
    # Redis
    - name: REDIS_HOST
      value: "redis.default.svc.cluster.local"
    - name: REDIS_PORT
      value: "6379"
    
    # Resource limits
    resources:
      requests:
        memory: "512Mi"
        cpu: "250m"
      limits:
        memory: "1Gi"
        cpu: "500m"
    
    # Liveness and readiness probes
    livenessProbe:
      httpGet:
        path: /actuator/health
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
    
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
```

---

## Summary

These templates provide ready-to-use code for implementing all T-602 requirements. Use them as starting points and adapt to your specific needs.

**Key Implementation Order:**
1. EncryptionUtil + AttributeConverter (foundation)
2. Update entities with encryption
3. Database migration
4. Public key endpoint
5. Secrets Manager integration
6. Database SSL
7. Key rotation
8. Kubernetes integration


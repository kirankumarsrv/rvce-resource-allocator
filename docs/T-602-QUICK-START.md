# T-602 Quick Start Setup Guide

This guide helps you set up and test the T-602 security features locally and in AWS.

---

## Quick Navigation

- **[Part 1: Local Development Setup](#part-1-local-development-setup)** - Get encryption working locally
- **[Part 2: AWS Secrets Manager Setup](#part-2-aws-secrets-manager-setup)** - Configure AWS
- **[Part 3: Testing Encryption](#part-3-testing-encryption)** - Verify encryption works
- **[Part 4: Key Generation](#part-4-key-generation)** - Create JWT keys
- **[Part 5: Integration Testing](#part-5-integration-testing)** - End-to-end testing

---

## Part 1: Local Development Setup

### Step 1.1: Generate Encryption Key

```bash
# Generate a 256-bit (32-byte) random encryption key
openssl rand -base64 32

# Output: something like: "7gFk3L9pQ2mX8vN1bJ5yH4wRtU6sA0dE+cF="
# Save this value - you'll need it in application.yml
```

### Step 1.2: Update Local Application Configuration

Edit `backend/src/main/resources/application.yaml`:

```yaml
spring:
  profiles:
    active: dev

---

spring:
  config:
    activate:
      on-profile: dev
  
  datasource:
    url: jdbc:postgresql://localhost:5432/scas_db?stringtype=unspecified&sslmode=disable
    username: scas
    password: scas_dev_password

# Encryption configuration for development
scas:
  encryption:
    # Use the key generated above
    key: 7gFk3L9pQ2mX8vN1bJ5yH4wRtU6sA0dE+cF=
  
  jwt:
    # For development: keys will be generated ephemeral if blank
    private-key-path: 
    public-key-path: 
    access-token-expiry-ms: 900000
    refresh-token-expiry-seconds: 604800
```

### Step 1.3: Add Dependencies

Update `backend/build.gradle.kts`:

```kotlin
dependencies {
    // Existing dependencies...
    
    // AES encryption and RSA for JWT
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    
    // AWS SDK (optional for local dev, required for production)
    implementation("software.amazon.awssdk:secretsmanager:2.25.0")
}
```

### Step 1.4: Create Encryption Utility

Create `backend/src/main/java/com/rvce/scas/security/EncryptionUtil.java` using the template from Part 2 of the code templates document.

### Step 1.5: Create AttributeConverter

Create `backend/src/main/java/com/rvce/scas/config/EncryptedStringConverter.java` using the template from Part 2 of the code templates document.

### Step 1.6: Apply Converter to Entity

Update `backend/src/main/java/com/rvce/scas/entity/User.java`:

```java
import com.rvce.scas.config.EncryptedStringConverter;
import jakarta.persistence.Convert;

@Entity
@Table(name = "users")
public class User {
    
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email", nullable = false, length = 500)
    private String email;
    
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "usn", length = 100)
    private String usn;
    
    // ... rest of entity
}
```

### Step 1.7: Create Database Migration

Create `backend/src/main/resources/db/migration/V6__enable_column_encryption.sql`:

```sql
-- Extend column lengths for encrypted data
ALTER TABLE users
ALTER COLUMN email TYPE VARCHAR(500),
ALTER COLUMN usn TYPE VARCHAR(100);
```

### Step 1.8: Verify Local Setup

```bash
cd backend

# Build the project
./gradlew clean build

# Start PostgreSQL (if not running)
docker run -d --name postgres \
  -e POSTGRES_USER=scas \
  -e POSTGRES_PASSWORD=scas_dev_password \
  -e POSTGRES_DB=scas_db \
  -p 5432:5432 \
  postgres:15

# Run the application
./gradlew bootRun
```

Watch for successful startup with no encryption errors.

---

## Part 2: AWS Secrets Manager Setup

### Step 2.1: Create AWS Account and Configure Credentials

```bash
# Configure AWS CLI with your credentials
aws configure

# Verify configuration
aws sts get-caller-identity
```

### Step 2.2: Create Secret for Development

```bash
# Generate a real encryption key
ENCRYPTION_KEY=$(openssl rand -base64 32)

# Create the AWS secret
aws secretsmanager create-secret \
  --name dev/scas/secrets \
  --region us-east-1 \
  --secret-string "{
    \"encryption-key\": \"$ENCRYPTION_KEY\",
    \"db-password\": \"scas_dev_password\",
    \"jwt-private-key\": \"\",
    \"jwt-public-key\": \"\"
  }"

echo "Created secret: dev/scas/secrets"
echo "Encryption Key: $ENCRYPTION_KEY"
```

### Step 2.3: Create Secret for Production

```bash
# Generate production keys (see Part 4 for JWT key generation)
PROD_ENCRYPTION_KEY=$(openssl rand -base64 32)
PROD_DB_PASSWORD="your_secure_prod_password"

# Read JWT keys from files (created in Part 4)
JWT_PRIVATE=$(cat jwt_private_b64.txt)
JWT_PUBLIC=$(cat jwt_public_b64.txt)

# Create production secret
aws secretsmanager create-secret \
  --name prod/scas/secrets \
  --region us-east-1 \
  --secret-string "{
    \"encryption-key\": \"$PROD_ENCRYPTION_KEY\",
    \"db-password\": \"$PROD_DB_PASSWORD\",
    \"jwt-private-key\": \"$JWT_PRIVATE\",
    \"jwt-public-key\": \"$JWT_PUBLIC\"
  }"

echo "Created secret: prod/scas/secrets"
```

### Step 2.4: Verify Secret Access

```bash
# Retrieve the secret
aws secretsmanager get-secret-value \
  --secret-id dev/scas/secrets \
  --region us-east-1

# Output should show encrypted content
```

### Step 2.5: Configure IAM Permissions

Create an IAM policy for local development:

```bash
cat > scas-secrets-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:*:secret:dev/scas/secrets-*",
        "arn:aws:secretsmanager:us-east-1:*:secret:prod/scas/secrets-*"
      ]
    }
  ]
}
EOF

# Attach to your IAM user
aws iam put-user-policy \
  --user-name your-username \
  --policy-name scas-secrets-policy \
  --policy-document file://scas-secrets-policy.json
```

### Step 2.6: Enable Spring Cloud Secrets Manager

Update `backend/build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.awspring.cloud:spring-cloud-aws-secrets-manager-config:2.4.4")
}
```

Create `backend/src/main/resources/application-aws.yml`:

```yaml
spring:
  config:
    import:
      - aws-secrets:${SCAS_SECRETS_NAME:dev/scas/secrets}

aws:
  secretsmanager:
    region: ${AWS_REGION:us-east-1}
```

Update `backend/src/main/resources/application.yaml`:

```yaml
spring:
  profiles:
    active: dev
  config:
    import:
      - aws-secrets:${SCAS_SECRETS_NAME:dev/scas/secrets}

scas:
  encryption:
    key: ${encryption-key}
  jwt:
    private-key: ${jwt-private-key}
    public-key: ${jwt-public-key}
```

---

## Part 3: Testing Encryption

### Step 3.1: Unit Test

Create `backend/src/test/java/com/rvce/scas/security/EncryptionUtilTest.java` using the template from Part 2 of the code templates.

Run tests:

```bash
cd backend
./gradlew test --tests EncryptionUtilTest -i
```

Expected output:
```
✓ testEncryptDecryptRoundTrip
✓ testEncryptionVariation  
✓ testNullHandling
✓ testEmptyStringHandling
✓ testVariousDataTypes
✓ testCorruptedCiphertext
```

### Step 3.2: Integration Test

Create `backend/src/test/java/com/rvce/scas/entity/UserEncryptionTest.java`:

```java
package com.rvce.scas.entity;

import com.rvce.scas.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserEncryptionTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testEmailEncryptedInDatabase() {
        // Create user with plaintext email
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setUsn("CSE21001");
        user.setPasswordHash("hashed_password");
        user.setActive(true);
        user.setFailedLoginCount((short) 0);

        // Save user (encryption happens automatically)
        User saved = userRepository.save(user);
        UUID userId = saved.getUserId();

        // Retrieve plaintext from database (raw query)
        String rawDbEmail = jdbcTemplate.queryForObject(
            "SELECT email FROM users WHERE user_id = ?",
            String.class,
            userId
        );

        // Verify email is encrypted in database
        assertNotEquals("john@example.com", rawDbEmail);
        assertTrue(rawDbEmail.length() > 40); // Encrypted data is much longer

        // Retrieve via JPA (decryption happens automatically)
        User retrieved = userRepository.findById(userId).orElseThrow();
        assertEquals("john@example.com", retrieved.getEmail());
        assertEquals("CSE21001", retrieved.getUsn());
    }
}
```

Run integration test:

```bash
cd backend
./gradlew test --tests UserEncryptionTest -i
```

### Step 3.3: Manual Database Verification

```bash
# Connect to PostgreSQL
psql -h localhost -U scas -d scas_db

# Create test user (application does this)
# SELECT * FROM users WHERE email LIKE '%@example.com';

# Output should show:
# Column email: contains encrypted base64 string (not plaintext)
# Example: "A7F3B2C9D1E4F5A6B7C8D9E0F1A2B3C4D5E6..."
```

---

## Part 4: Key Generation

### Step 4.1: Generate RSA Key Pair for JWT

```bash
# Create a directory for keys
mkdir -p jwt_keys
cd jwt_keys

# Generate 2048-bit RSA private key
openssl genrsa -out jwt_private.pem 2048

# Extract public key from private key
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem

# Verify keys
cat jwt_private.pem  # Should show "-----BEGIN RSA PRIVATE KEY-----"
cat jwt_public.pem   # Should show "-----BEGIN PUBLIC KEY-----"
```

### Step 4.2: Encode Keys for AWS Secrets Manager

```bash
# AWS Secrets Manager expects base64-encoded values
base64 -w0 jwt_private.pem > jwt_private_b64.txt
base64 -w0 jwt_public.pem > jwt_public_b64.txt

# View encoded keys (for copy/paste into AWS console)
cat jwt_private_b64.txt
cat jwt_public_b64.txt
```

### Step 4.3: Store Keys Locally for Development

Create `backend/config/jwt/jwt_private.pem`:

```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA7jF3k9w... (content from jwt_private.pem)
-----END RSA PRIVATE KEY-----
```

Update application.yml:

```yaml
scas:
  jwt:
    private-key-path: file:./config/jwt/jwt_private.pem
    public-key-path: file:./config/jwt/jwt_public.pem
```

### Step 4.4: Test JWT Token Generation

Create `backend/src/test/java/com/rvce/scas/security/JwtTokenProviderTest.java`:

```java
package com.rvce.scas.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testGenerateAndValidateToken() {
        // Generate token
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        List<String> roles = Arrays.asList("STUDENT", "USER");

        String token = jwtTokenProvider.generateAccessToken(userId, email, roles);

        // Verify token is not empty
        assertNotNull(token);
        assertTrue(token.length() > 0);

        // Verify token format (3 parts separated by dots: header.payload.signature)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        // Verify token can be validated
        boolean valid = jwtTokenProvider.validateToken(token);
        assertTrue(valid);

        // Verify token contains expected claims
        var claims = Jwts.parserBuilder()
            .setSigningKey(jwtTokenProvider.getPublicKey())
            .build()
            .parseClaimsJws(token)
            .getBody();

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.get("email"));
    }
}
```

Run test:

```bash
cd backend
./gradlew test --tests JwtTokenProviderTest -i
```

---

## Part 5: Integration Testing

### Step 5.1: Test Encryption with AWS Secrets Manager

```bash
# Set environment variables to use AWS
export AWS_REGION=us-east-1
export SCAS_SECRETS_NAME=dev/scas/secrets

# Build and run with AWS profile
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev,aws'
```

Check logs for:
```
Loading secrets from AWS Secrets Manager
Secrets loaded successfully
Encryption utility initialized
```

### Step 5.2: Test Database SSL Connection

For production database (AWS RDS):

```bash
# Update application.yml for production
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=your-rds-instance.xxx.us-east-1.rds.amazonaws.com
export DB_USER=scas
export DB_PASSWORD=your_password

# Download RDS CA certificate
wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
  -O backend/certs/rds-ca.pem

# Update application to use certificate
# See docker build section for how to copy into container

# Run application
./gradlew bootRun
```

### Step 5.3: Test Public Key Endpoint

```bash
# Start application
cd backend
./gradlew bootRun

# In another terminal, test the endpoint
curl http://localhost:8080/api/public-keys/jwt.pub

# Should return:
# -----BEGIN PUBLIC KEY-----
# MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
# -----END PUBLIC KEY-----
```

### Step 5.4: Test JWKS Endpoint

```bash
# Test JWKS endpoint
curl http://localhost:8080/api/public-keys/jwks.json

# Should return:
# {
#   "keys": [
#     {
#       "kid": "2024-Q1",
#       "kty": "RSA",
#       "alg": "RS256",
#       "use": "sig",
#       "n": "xGOr-H...",
#       "e": "AQAB"
#     }
#   ]
# }
```

### Step 5.5: Test Token Validation in Another Service

```java
// In a different service/pod, validate the token

import io.jsonwebtoken.Jwts;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class TokenValidator {
    
    public boolean validateToken(String token, String publicKeyUrl) throws Exception {
        // Fetch public key from endpoint
        URL url = new URL(publicKeyUrl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder keyContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            keyContent.append(line).append("\n");
        }
        reader.close();
        
        // Parse PEM public key
        String publicKeyPem = keyContent.toString()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        
        byte[] decodedKey = Base64.getDecoder().decode(publicKeyPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        
        // Validate token
        try {
            Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Part 6: Troubleshooting

### Issue: "Encryption key not found in application configuration"

**Solution:**
```yaml
# Make sure scas.encryption.key is set in application.yml
scas:
  encryption:
    key: ${ENCRYPTION_KEY:default-dev-key}  # Add default for development
```

### Issue: "AWS Secrets Manager connection refused"

**Solution:**
```bash
# Verify AWS credentials
aws sts get-caller-identity

# Verify secret exists
aws secretsmanager describe-secret --secret-id dev/scas/secrets

# Check AWS region
export AWS_REGION=us-east-1
```

### Issue: "Database column too small for encrypted data"

**Solution:**
```sql
-- Run Flyway migration to extend columns
-- File: V6__enable_column_encryption.sql

ALTER TABLE users
ALTER COLUMN email TYPE VARCHAR(500);
```

### Issue: "RSA key size must be between 1024 and 16384"

**Solution:**
```bash
# Regenerate 2048-bit keys
openssl genrsa -out jwt_private.pem 2048
```

### Issue: "JWKS endpoint returns empty keys array"

**Solution:**
```java
// Make sure JwtTokenProvider.getJwks() is implemented
public JwksResponse getJwks() {
    JwksResponse response = new JwksResponse();
    // Add current public key to response
    response.setKeys(getCurrentAndPreviousKeys());
    return response;
}
```

---

## Part 7: Deployment Checklist

### Before Deploying to Production

- [ ] AWS Secrets Manager configured with prod/* secrets
- [ ] RDS database uses SSL (sslmode=require)
- [ ] RDS CA certificate bundled in Docker image
- [ ] Encryption key is 256-bit (32 bytes) in base64
- [ ] RSA keys are 2048-bit
- [ ] Database migration applied (columns extended)
- [ ] Public key endpoint is accessible externally
- [ ] JWKS endpoint returns all active keys
- [ ] Key rotation scheduler is enabled
- [ ] CloudWatch logs don't contain plaintext secrets
- [ ] IAM role allows pod to read from Secrets Manager
- [ ] External Secrets Operator installed in K8s cluster
- [ ] K8s Secret mounted correctly in pod

### Monitoring and Alerts

```bash
# Set up CloudWatch alerts for encryption failures
aws cloudwatch put-metric-alarm \
  --alarm-name scas-encryption-failures \
  --alarm-description "Alert on encryption errors" \
  --metric-name DecryptionErrors \
  --namespace SCAS \
  --statistic Sum \
  --period 300 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold

# Monitor key rotation
aws cloudwatch put-metric-alarm \
  --alarm-name scas-key-rotation \
  --alarm-description "Alert on key rotation failures" \
  --metric-name KeyRotationFailures \
  --namespace SCAS
```

---

## Quick Commands Reference

```bash
# Generate encryption key
openssl rand -base64 32

# Generate JWT keys
openssl genrsa -out jwt_private.pem 2048
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem

# Encode for AWS
base64 -w0 jwt_private.pem > jwt_private_b64.txt

# Create AWS secret
aws secretsmanager create-secret --name dev/scas/secrets --secret-string "{...}"

# Retrieve AWS secret
aws secretsmanager get-secret-value --secret-id dev/scas/secrets

# Test public key endpoint
curl http://localhost:8080/api/public-keys/jwt.pub

# Test token validation
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/endpoint

# Run encryption tests
./gradlew test --tests EncryptionUtilTest
```

---

## Next Steps

1. ✅ Complete Part 1 (Local Setup)
2. ✅ Complete Part 2 (AWS Setup)
3. ✅ Complete Part 3 (Testing)
4. ✅ Complete Part 4 (Key Generation)
5. ✅ Complete Part 5 (Integration Testing)
6. Deploy to dev/staging (non-production) first
7. Run load tests to verify performance
8. Deploy to production
9. Set up monitoring and alerts
10. Document runbooks for key rotation


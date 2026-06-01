# T-602: Encryption & Secrets Implementation Guide

## Overview

This document provides a comprehensive explanation and implementation plan for T-602, which covers:
1. **AES-256 Column Encryption** for sensitive data at rest
2. **SSL Database Connections** for data in transit
3. **AWS Secrets Manager** for secret management
4. **JWT RS256 Key Pair** with key rotation
5. **Kubernetes Integration** for secrets synchronization

---

## Part 1: Detailed Explanation of Each Requirement

### 1. AES-256 Column Encryption with JPA AttributeConverter

#### What It Does:
- Encrypts sensitive columns (USN, email, phone) in the database at the Java application level
- Data is encrypted **before** being stored in the database and decrypted **after** retrieval
- Uses AES (Advanced Encryption Standard) with 256-bit keys in GCM mode (authenticated encryption)

#### Why It Matters:
- **Defense in Depth**: Even if someone gains unauthorized database access, they cannot read encrypted fields
- **Regulatory Compliance**: Satisfies data protection regulations (GDPR, PII handling)
- **Transparent**: Encryption/decryption happens automatically in JPA

#### Technical Details:

**AES-256-GCM Encryption Mode:**
- **AES**: Advanced Encryption Standard (industry standard)
- **256**: 256-bit encryption key (extremely strong)
- **GCM** (Galois/Counter Mode): Provides both confidentiality AND integrity verification
- **NoPadding**: GCM handles padding internally

**JPA AttributeConverter:**
- Interface that Spring Data JPA uses to convert Java types to/from database types
- Automatically called on `@PrePersist` (before INSERT) and when reading entities
- Allows transparent encryption without changing business logic

**How It Works:**
```
1. Application creates: user.setEmail("student@example.com")
2. JPA AttributeConverter.convertToDatabaseColumn() is called
3. Encryption happens: plaintext → ciphertext (e.g., "A7F3B2C9D1E...")
4. Database stores: encrypted ciphertext
5. On read: JPA AttributeConverter.convertToEntityAttribute() is called
6. Decryption happens: ciphertext → plaintext
7. Application sees: "student@example.com"
```

#### Key Generation:
- Use `SecureRandom` to generate 256-bit (32-byte) random key
- Store in AWS Secrets Manager (never hardcode)
- Same key used for encryption/decryption across all instances

#### Data Format:
```
Encrypted data format: [IV(16 bytes)][Ciphertext][AuthTag(16 bytes)]
- IV (Initialization Vector): Random for each encryption
- AuthTag: Proves data wasn't tampered with
```

---

### 2. Database Connection with SSL/TLS

#### What It Does:
- Encrypts all communication between application and PostgreSQL database
- Prevents passwords and data from being transmitted in plaintext over network

#### Why It Matters:
- **Network Security**: Even if someone intercepts traffic, it's encrypted
- **AWS RDS Requirement**: Production RDS instances require SSL connections
- **Zero Trust Network**: Never assume network traffic is secure

#### Configuration:
```
JDBC URL: jdbc:postgresql://host:5432/database?sslmode=require
```

**SSL Modes:**
- `require`: Connection MUST use SSL (fails if not available)
- `prefer`: Tries SSL first, falls back to plaintext (not recommended)
- `verify-full`: SSL + certificate validation (highest security)

---

### 3. AWS Secrets Manager Integration

#### What It Does:
- Centralized secret storage outside your application codebase
- Manages: DB passwords, API keys, encryption keys, JWT keys, email credentials
- Prevents secrets from appearing in code, logs, or environment variables

#### Why It Matters:
- **No Hardcoded Secrets**: If code is leaked, secrets are not exposed
- **Auditable**: AWS logs who accessed which secrets and when
- **Automatic Rotation**: Secrets can be rotated without redeploying
- **Access Control**: IAM policies control which services can access which secrets

#### Secret Structure:
```json
{
  "db-password": "complex_password_123",
  "encryption-key": "base64_encoded_256_bit_key",
  "jwt-private-key": "-----BEGIN RSA PRIVATE KEY-----\n...",
  "jwt-public-key": "-----BEGIN PUBLIC KEY-----\n...",
  "email-password": "app_specific_password"
}
```

---

### 4. Spring Cloud AWS Secrets Manager Configuration

#### What It Does:
- Provides a Spring Boot integration that:
  - Loads secrets from AWS Secrets Manager at application startup
  - Injects secrets as `@Value` properties
  - Refreshes secrets periodically without restart

#### How It Works:
```
Application Startup:
1. Spring Cloud loads `application.yml`
2. Sees `${AWS_SECRET:default-value}` placeholder
3. Connects to AWS Secrets Manager
4. Retrieves secret value
5. Injects into bean via @Value annotation
6. During operation: if secret is updated in AWS, app can refresh it
```

#### Benefits:
- **No Restart Required**: Update secrets in AWS, refresh in app
- **Hierarchical**: Support for secret versioning and naming conventions
- **Type-Safe**: Can convert JSON secrets to Java objects

---

### 5. Kubernetes External Secrets Operator

#### What It Does:
- Synchronizes AWS Secrets Manager ↔ Kubernetes Secrets
- Keeps K8s cluster in sync with AWS master secrets
- Injects secrets as environment variables into pods

#### Architecture:
```
AWS Secrets Manager
       ↓ (External Secrets Operator)
Kubernetes Secret (my-app-secrets)
       ↓ (Pod mount or env var)
Container Environment Variables
```

#### Why It Matters:
- **Single Source of Truth**: AWS Secrets Manager is the master
- **Automatic Sync**: Changes in AWS automatically propagate to K8s
- **Multi-Environment**: Different secrets for dev/staging/prod in same K8s cluster
- **Pod Isolation**: Each pod only gets secrets it needs

---

### 6. JWT RS256 Key Pair

#### What It Does:
- Uses **RSA public/private key pair** for JWT signing and verification
- Better than symmetric keys (HMAC) for distributed systems

#### RS256 vs HMAC (HS256):

**HMAC (HS256 - Symmetric):**
```
┌─────────────┐
│ Shared Key  │ ← Everyone has the same key
│ (Secret)    │
└─────────────┘
 Used for both signing AND verification
 Problem: If key is leaked, everyone can forge tokens
```

**RS256 (Asymmetric):**
```
┌──────────────┐              ┌─────────────┐
│ Private Key  │              │ Public Key  │
│ (Secret)     │              │ (Shared)    │
└──────────────┘              └─────────────┘
   Signing Only          Verification Only
```

#### 2048-bit RSA Key:
- **2048-bit**: Very strong encryption (mathematically hard to break)
- **RSA**: Industry standard asymmetric encryption
- **Key Pair**: One private (secret), one public (shareable)

#### How Token Validation Works:

**Service A (Issues Token):**
```
1. User logs in with password
2. Service A signs JWT using PRIVATE key
3. Returns JWT to client: "eyJhbGc.eyJzdWI.SflKxw..."
```

**Service B (Validates Token):**
```
1. Client sends JWT in Authorization header
2. Service B fetches PUBLIC key from Service A's /public-keys/jwt.pub
3. Verifies signature using PUBLIC key
4. If signature valid → token is authentic and unchanged
5. If signature invalid → token was forged or tampered with
```

#### Public Key Endpoint:
```
GET /public-keys/jwt.pub
Response:
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7jF...
-----END PUBLIC KEY-----
```

---

### 7. Key Rotation Strategy

#### What It Does:
- Regularly generates new RSA key pairs
- Maintains an overlap period where both old and new keys are valid
- Prevents service disruption during key changes

#### Problem It Solves:
```
Without rotation:
┌─────────────────────────────────────┐
│     Same key for 1+ years           │
│ Risk: If key is ever compromised,   │
│       attacker can forge all tokens │
│       back to year 1                │
└─────────────────────────────────────┘

With rotation (quarterly):
┌───────┬───────┬───────┬───────┐
│ Key 1 │ Key 2 │ Key 3 │ Key 4 │
│ Q1    │ Q2    │ Q3    │ Q4    │
└───────┴───────┴───────┴───────┘
Risk window: limited to ~3 months
```

#### Rotation Process:

**Phase 1: Generate New Key (Quarterly)**
```
Monday: Generate new RSA key pair
- New private key → AWS Secrets Manager
- New public key → /public-keys/jwt.pub endpoint
```

**Phase 2: Overlap Period (48 hours)**
```
Monday 00:00 - Wednesday 00:00 (48 hours)
- New tokens: signed with NEW private key
- Old tokens: still accepted (verified with OLD public key)
- Both public keys available on endpoint
```

**Phase 3: Phase Out Old Key**
```
Wednesday 00:00 onwards:
- Only new tokens issued
- Existing tokens with old signature still valid until natural expiry
- Old public key gradually removed from endpoint
```

#### JWKS Endpoint (JSON Web Key Set):
```
GET /public-keys/jwks.json
Response:
{
  "keys": [
    {
      "kid": "2024-Q1",
      "use": "sig",
      "alg": "RS256",
      "kty": "RSA",
      "n": "xGOr-H...",
      "e": "AQAB"
    },
    {
      "kid": "2024-Q2",
      "use": "sig",
      "alg": "RS256",
      "kty": "RSA",
      "n": "yHPu-G...",
      "e": "AQAB"
    }
  ]
}
```

**Benefits:**
- Clients can discover active keys automatically
- Clients cache keys and validate against all available keys
- Smooth key rotation without API versioning

---

## Part 2: Project Analysis

### Current Project Structure

Your project is a Spring Boot 3.5.13 application with:
- **Framework**: Spring Boot, Spring Security
- **Database**: PostgreSQL 15
- **ORM**: JPA/Hibernate
- **Authentication**: JWT (JJWT library v0.12.6)
- **Caching**: Redis
- **Deployment**: Docker containers + Kubernetes-ready

### Sensitive Data Requiring Encryption

Based on code analysis:

**User Entity:**
- `email` - User login identifier and contact info
- `usn` - University Serial Number (student ID)
- (Could add: phone, emergency contact)

**ExamStudent Entity:**
- `usn` - Student ID
- `studentName` - PII
- (Could add: email, phone)

---

## Part 3: Step-by-Step Implementation Plan

### Phase 1: Foundation Setup

#### Step 1.1: Add AWS SDK Dependencies
Add to `build.gradle.kts`:
```kotlin
implementation("software.amazon.awssdk:secretsmanager:2.25.0")
implementation("io.awspring.cloud:spring-cloud-aws-secrets-manager-config:2.4.4")
```

#### Step 1.2: Create Encryption Utility Class
Location: `backend/src/main/java/com/rvce/scas/security/EncryptionUtil.java`

Key features:
- Generate/load 256-bit AES keys
- Encrypt plaintext with AES-256-GCM
- Decrypt ciphertext with AES-256-GCM
- Validate authentication tag

#### Step 1.3: Create JPA AttributeConverter
Location: `backend/src/main/java/com/rvce/scas/config/EncryptedStringConverter.java`

Features:
- Implement `AttributeConverter<String, String>`
- Inject EncryptionUtil
- Auto-encrypt on `convertToDatabaseColumn()`
- Auto-decrypt on `convertToEntityAttribute()`

---

### Phase 2: Apply Encryption to Entities

#### Step 2.1: Annotate Sensitive Columns
Modify entity classes:

**User.java:**
```java
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "email", nullable = false, length = 255)
private String email;

@Convert(converter = EncryptedStringConverter.class)
@Column(name = "usn", length = 20)
private String usn;
```

**ExamStudent.java:**
```java
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "usn", nullable = false, length = 20)
private String usn;
```

#### Step 2.2: Update Database Schema
Flyway migration: `V6__enable_column_encryption.sql`

Extend VARCHAR column lengths for encrypted data:
```sql
-- Encrypted data is larger than plaintext
-- "student@example.com" (19 chars) 
-- → "A7F3B2C9D1E4F5A6B7C8D9E0F1A2B3C4..." (80+ chars)

ALTER TABLE users 
ALTER COLUMN email TYPE VARCHAR(500),
ALTER COLUMN usn TYPE VARCHAR(100);

ALTER TABLE exam_students
ALTER COLUMN usn TYPE VARCHAR(100);
```

---

### Phase 3: Secrets Management

#### Step 3.1: Configure AWS Secrets Manager
**Secret Name**: `prod/scas/secrets`

**Secret Value (JSON)**:
```json
{
  "encryption-key": "BPEWj/lL...base64-256-bit-key...QWp+aM=",
  "db-password": "complex_postgres_password_123",
  "jwt-private-key": "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...",
  "jwt-public-key": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0...",
  "email-password": "app_specific_gmail_password"
}
```

#### Step 3.2: Add Spring Cloud Secrets Manager Config
Create: `backend/src/main/resources/application-secrets.yml`

```yaml
aws:
  secretsmanager:
    endpoint: https://secretsmanager.us-east-1.amazonaws.com
    region: us-east-1

scas:
  encryption:
    # Loaded from AWS Secrets Manager
    key: ${encryption-key}
  jwt:
    private-key: ${jwt-private-key}
    public-key: ${jwt-public-key}
```

#### Step 3.3: Update Main Config
```yaml
spring:
  config:
    import: aws-secrets:prod/scas/secrets
```

---

### Phase 4: Database SSL/TLS

#### Step 4.1: Update Database URL
In `application.yml`:

**Development (local):**
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/scas_db?stringtype=unspecified&sslmode=disable
```

**Production (AWS RDS):**
```yaml
datasource:
  url: jdbc:postgresql://prod-rds.xxx.us-east-1.rds.amazonaws.com:5432/scas_db?sslmode=require&sslrootcert=/opt/certs/rds-ca.pem
```

#### Step 4.2: SSL Certificate Setup (Production)
```bash
# For AWS RDS: download CA certificate
wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem

# Copy to Docker image
COPY ./certs/rds-ca.pem /opt/certs/rds-ca.pem
```

---

### Phase 5: JWT Key Pair Management

#### Step 5.1: RSA Key Pair Generation
Use OpenSSL to generate initial key pair:

```bash
# Generate 2048-bit RSA private key
openssl genrsa -out jwt_private.pem 2048

# Extract public key
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem

# Encode for AWS Secrets Manager (base64)
cat jwt_private.pem | base64 -w0 > jwt_private_b64.txt
cat jwt_public.pem | base64 -w0 > jwt_public_b64.txt
```

#### Step 5.2: Store Keys in Secrets Manager
Store the base64-encoded private key in AWS Secrets Manager.

#### Step 5.3: Create Public Key Endpoint
Create controller: `PublicKeyController.java`

```java
@RestController
@RequestMapping("/api/public-keys")
@CrossOrigin
public class PublicKeyController {
    
    @GetMapping("/jwt.pub")
    public ResponseEntity<String> getJwtPublicKey() {
        // Load from AWS Secrets Manager
        String publicKeyPem = jwtTokenProvider.getPublicKeyPem();
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(publicKeyPem);
    }
    
    @GetMapping("/jwks.json")
    public ResponseEntity<JwksResponse> getJwks() {
        // Return current and previous public keys
        return ResponseEntity.ok(jwtTokenProvider.getJwks());
    }
}
```

---

### Phase 6: Key Rotation Implementation

#### Step 6.1: Scheduled Key Rotation Task
Create: `KeyRotationScheduler.java`

```java
@Component
@Slf4j
public class KeyRotationScheduler {
    
    @Scheduled(cron = "0 0 0 1 * *") // First day of each month
    public void rotateKeys() {
        // 1. Generate new RSA key pair
        KeyPair newKeyPair = generateNewKeyPair();
        
        // 2. Store in AWS Secrets Manager with version
        String newKeyId = storeNewKey(newKeyPair);
        
        // 3. Update rotation metadata
        KeyRotationMetadata metadata = new KeyRotationMetadata();
        metadata.setActiveKeyId(newKeyId);
        metadata.setActivationTime(Instant.now());
        metadata.setDeprecationTime(Instant.now().plusSeconds(48 * 3600)); // 48 hours
        keyRotationRepository.save(metadata);
        
        log.info("New JWT key pair generated and activated: {}", newKeyId);
    }
}
```

#### Step 6.2: Multi-Key Validation
Update `JwtTokenProvider`:

```java
public boolean validateToken(String token) {
    try {
        Claims claims = Jwts.parserBuilder()
                .setSigningKeyResolver(new SigningKeyResolver() {
                    @Override
                    public Key resolveSigningKey(JwtParser parser, Claims claims) {
                        // Try current key first
                        if (isValidWithKey(token, currentPublicKey)) {
                            return currentPublicKey;
                        }
                        // Try previous key (in overlap period)
                        if (isValidWithKey(token, previousPublicKey)) {
                            return previousPublicKey;
                        }
                        throw new SignatureException("No valid key found");
                    }
                })
                .build()
                .parseClaimsJws(token);
        return true;
    } catch (JwtException e) {
        return false;
    }
}
```

---

### Phase 7: Kubernetes Integration

#### Step 7.1: External Secrets Operator Installation
```bash
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
```

#### Step 7.2: AWS IAM Role Setup
```yaml
# Create IAM role for EKS service account
apiVersion: iam.cnpg.io/v1alpha1
kind: IAMRole
metadata:
  name: scas-app-role
spec:
  statements:
    - Effect: Allow
      Action:
        - secretsmanager:GetSecretValue
        - secretsmanager:DescribeSecret
      Resource:
        - arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:prod/scas/secrets-*
```

#### Step 7.3: External Secrets Resource
Create: `k8s/external-secrets.yaml`

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets
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

apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: scas-secrets
spec:
  secretStoreRef:
    name: aws-secrets
    kind: SecretStore
  target:
    name: scas-secrets-k8s
  data:
    - secretKey: encryption-key
      remoteRef:
        key: prod/scas/secrets
        property: encryption-key
    - secretKey: db-password
      remoteRef:
        key: prod/scas/secrets
        property: db-password
    - secretKey: jwt-private-key
      remoteRef:
        key: prod/scas/secrets
        property: jwt-private-key
```

#### Step 7.4: Pod Environment Variable Injection
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: scas-app
spec:
  containers:
  - name: scas
    image: scas-app:latest
    env:
    - name: ENCRYPTION_KEY
      valueFrom:
        secretKeyRef:
          name: scas-secrets-k8s
          key: encryption-key
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: scas-secrets-k8s
          key: db-password
    - name: JWT_PRIVATE_KEY
      valueFrom:
        secretKeyRef:
          name: scas-secrets-k8s
          key: jwt-private-key
```

---

## Part 4: Implementation Checklist

### Phase 1: Foundation
- [ ] Add AWS SDK and Spring Cloud Secrets Manager dependencies
- [ ] Create EncryptionUtil class with AES-256-GCM support
- [ ] Create EncryptedStringConverter JPA converter
- [ ] Unit test encryption/decryption

### Phase 2: Entity Updates
- [ ] Add @Convert annotations to User entity (email, usn)
- [ ] Add @Convert annotations to ExamStudent entity (usn)
- [ ] Create Flyway migration to extend column lengths
- [ ] Test entity persistence and retrieval

### Phase 3: Secrets Management
- [ ] Create AWS Secrets Manager secret with all required values
- [ ] Update application.yml for Spring Cloud Secrets Manager
- [ ] Configure AWS credentials in development environment
- [ ] Test secrets loading at application startup

### Phase 4: Database SSL
- [ ] Update connection strings for production RDS
- [ ] Download RDS CA certificate
- [ ] Update Docker image to include certificate
- [ ] Test SSL connection

### Phase 5: JWT Key Management
- [ ] Generate RSA key pair locally
- [ ] Store in AWS Secrets Manager
- [ ] Create Public Key endpoint controller
- [ ] Implement JWKS endpoint
- [ ] Update JwtTokenProvider to load keys from Secrets Manager

### Phase 6: Key Rotation
- [ ] Create KeyRotationScheduler
- [ ] Create KeyRotationMetadata entity and repository
- [ ] Implement multi-key validation in JwtTokenProvider
- [ ] Test key rotation process

### Phase 7: Kubernetes
- [ ] Install External Secrets Operator
- [ ] Create IAM role for service account
- [ ] Create SecretStore and ExternalSecret resources
- [ ] Update deployment manifests
- [ ] Test secrets synchronization

---

## Part 5: Security Best Practices

### Development Environment
```yaml
# application-dev.yml
scas:
  encryption:
    key: ${ENCRYPTION_KEY:dev-key-for-testing-only}  # Override in environment
  jwt:
    private-key: ${JWT_PRIVATE_KEY:}  # Will generate ephemeral key if blank
```

### Production Environment
```yaml
# application-prod.yml (no defaults!)
scas:
  encryption:
    key: ${ENCRYPTION_KEY}  # MUST be set, no default
  jwt:
    private-key: ${JWT_PRIVATE_KEY}  # MUST be set
  database:
    ssl-mode: require
    ssl-root-cert: /opt/certs/rds-ca.pem
```

### Logging Security
```java
// NEVER log sensitive data
// BAD:
log.info("User email: {}", user.getEmail());

// GOOD:
log.info("User login: {}", user.getUserId());
```

### Backup and Recovery
- Backup encryption keys to AWS Backup service
- Test key recovery procedures quarterly
- Maintain offline encrypted backups of master keys
- Document key recovery runbook

---

## Part 6: Testing Strategy

### Unit Tests
```java
@Test
void testEncryptionDecryption() {
    String plaintext = "student@example.com";
    String encrypted = encryptionUtil.encrypt(plaintext);
    String decrypted = encryptionUtil.decrypt(encrypted);
    
    assertNotEquals(plaintext, encrypted);
    assertEquals(plaintext, decrypted);
}

@Test
void testAttributeConverterIntegration() {
    User user = new User();
    user.setEmail("student@example.com");
    User saved = userRepository.save(user);
    
    // Verify database stores encrypted data
    String rawDbValue = jdbcTemplate.queryForObject(
        "SELECT email FROM users WHERE user_id = ?",
        String.class,
        saved.getUserId()
    );
    assertNotEquals("student@example.com", rawDbValue);
}
```

### Integration Tests
- Test with real AWS Secrets Manager in test account
- Verify JWT token validation with public key endpoint
- Test key rotation without service restart

### Load Tests
- Ensure encryption/decryption doesn't become bottleneck
- Validate performance with large datasets

---

## Part 7: Deployment Timeline

### Week 1: Foundation
- Implement encryption utilities
- Add dependencies
- Create converters

### Week 2: Entity Updates
- Apply encryption to entities
- Database migration
- Testing

### Week 3: Secrets Management
- AWS Secrets Manager setup
- Spring Cloud integration
- Development environment testing

### Week 4: JWT & SSL
- Key pair generation
- Public key endpoint
- SSL configuration

### Week 5: Key Rotation
- Scheduler implementation
- Multi-key validation
- Testing

### Week 6: Kubernetes
- External Secrets Operator setup
- IAM roles
- Deployment manifests

### Week 7: Testing & Hardening
- Security testing
- Load testing
- Documentation

---

## Part 8: Troubleshooting Guide

### Issue: Decryption fails after restart
**Cause**: Different encryption key loaded
**Solution**: Verify encryption key is loaded from AWS Secrets Manager consistently

### Issue: Tokens not validating across pods
**Cause**: Different RSA keys generated per pod
**Solution**: Load keys from AWS Secrets Manager, not generated locally

### Issue: Database connection fails with SSL
**Cause**: Missing CA certificate
**Solution**: Ensure RDS CA certificate is mounted in Docker image

### Issue: Huge performance degradation
**Cause**: Encryption/decryption on every query
**Solution**: Use JPA caching, encrypt only on write not every query

---

## Conclusion

This implementation provides:
- ✅ **Encryption at rest**: AES-256-GCM for sensitive columns
- ✅ **Encryption in transit**: SSL/TLS for database and API
- ✅ **Secret management**: AWS Secrets Manager with Spring integration
- ✅ **Strong authentication**: RS256 JWT with asymmetric keys
- ✅ **Key rotation**: Quarterly rotation with overlap period
- ✅ **Kubernetes ready**: External Secrets Operator integration
- ✅ **Zero secrets in code**: All secrets externalized


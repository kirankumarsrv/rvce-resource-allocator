# SCAS Java Documentation Reference - Complete JavaDocs Guide

This document provides comprehensive JavaDoc documentation for all classes and methods in the SCAS backend codebase, organized by package.

## Table of Contents

1. [Controller Layer](#controller-layer)
2. [Service Layer](#service-layer)
3. [Exception Handling](#exception-handling)
4. [Security & RBAC](#security--rbac)
5. [Test Classes](#test-classes)

---

## Controller Layer

### backend/src/main/java/com/rvce/scas/controller/AuthController.java

**Class: `AuthController`**
- **Purpose**: Exposes the authentication API for issuing, renewing, and revoking tokens.
- **Package**: `com.rvce.scas.controller`
- **Annotations**: `@RestController`, `@RequestMapping("/api/auth")`, `@RequiredArgsConstructor`, `@Tag(name = "Authentication")`

#### Method: `login(LoginRequest request)`
```java
@PostMapping("/login")
@Operation(summary = "Authenticate and get JWT tokens")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request)
```
- **Purpose**: Authenticates a user and returns the access and refresh token pair.
- **Parameters**: 
  - `request` (LoginRequest): Login credentials submitted by the client (email and password in JSON body)
- **Returns**: `ResponseEntity<LoginResponse>` - A login response containing bearer token metadata
- **HTTP Status**: 200 OK on success
- **Security Notes**: 
  - Credentials are sent in JSON body (not URL query) to avoid logging secrets
  - Ensure frontend sends over HTTPS
  - Client stores refresh token securely (not in localStorage if XSS risk exists)
  - Response includes `accessToken` (short-lived JWT) and `refreshToken` (opaque ID)
- **Success Response Example**:
  ```json
  {
    "accessToken": "eyJhbGc...",
    "refreshToken": "uuid-string",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```

#### Method: `refresh(RefreshRequest request)`
```java
@PostMapping("/refresh")
@Operation(summary = "Get new access token using refresh token")
public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request)
```
- **Purpose**: Rotates the refresh token and returns a new access token.
- **Parameters**:
  - `request` (RefreshRequest): Contains `userId` and opaque refresh token id
- **Returns**: `ResponseEntity<LoginResponse>` - A new token pair for continued access
- **HTTP Status**: 200 OK on success
- **Process**:
  1. Validates the opaque refresh token id against Redis server-side state
  2. Loads user and rebuilds authorities
  3. Issues new access token
  4. Rotates refresh token (old is deleted, new is generated)
- **Exceptions Thrown**:
  - `InvalidTokenException`: If refresh token is invalid, expired, or user is disabled

#### Method: `logout(JwtPrincipal principal, String authHeader, LogoutRequest request)`
```java
@PostMapping("/logout")
@Operation(summary = "Logout and blacklist tokens")
public ResponseEntity<Void> logout(
    @AuthenticationPrincipal JwtPrincipal principal,
    @RequestHeader(value = "Authorization") String authHeader,
    @Valid @RequestBody LogoutRequest request)
```
- **Purpose**: Revokes the current device session and blacklists the presented tokens.
- **Parameters**:
  - `principal` (JwtPrincipal): The authenticated user making the request
  - `authHeader` (String): Bearer token supplied in the Authorization header
  - `request` (LogoutRequest): Logout payload containing the refresh token id
- **Returns**: `ResponseEntity<Void>` - Empty successful response (204 No Content or 200 OK)
- **Security Notes**: 
  - Refresh token is now in request body (not URL query parameter)
  - This prevents token leakage via logs, browser history, Referer headers
  - Token blacklisting expires when the original token would have naturally expired
- **Process**:
  1. Extracts bearer token from Authorization header
  2. Calls AuthService.logout() to blacklist access token and revoke refresh token
  3. Token blacklisting tracked in Redis

#### Method: `logoutAll(JwtPrincipal principal, String authHeader)`
```java
@PostMapping("/logout-all")
@Operation(summary = "Logout from all devices")
public ResponseEntity<Void> logoutAll(
    @AuthenticationPrincipal JwtPrincipal principal,
    @RequestHeader("Authorization") String authHeader)
```
- **Purpose**: Revokes every active session for the authenticated user.
- **Parameters**:
  - `principal` (JwtPrincipal): The authenticated user making the request
  - `authHeader` (String): Bearer token supplied in the Authorization header
- **Returns**: `ResponseEntity<Void>` - Empty successful response
- **Note**: Does not require a refresh token since all devices are revoked server-side
- **Process**:
  1. Extracts bearer token from Authorization header
  2. Calls AuthService.logoutAllDevices() to revoke all sessions
  3. All refresh tokens for the user are deleted from Redis

---

### backend/src/main/java/com/rvce/scas/controller/AdminController.java

**Class: `AdminController`**
- **Purpose**: Handles admin-facing endpoints for operational and audit visibility.
- **Package**: `com.rvce.scas.controller`
- **Annotations**: `@RestController`, `@RequestMapping("/api/admin")`
- **Security**: Requires `ROLE_ADMIN` or `ROLE_SUPER_ADMIN`

#### Method: `auditLogs()`
```java
@GetMapping("/audit-logs")
public ResponseEntity<Map<String, String>> auditLogs()
```
- **Purpose**: Returns a lightweight success payload for the audit log endpoint.
- **Parameters**: None
- **Returns**: `ResponseEntity<Map<String, String>>` - HTTP 200 response with a simple status marker
- **Response Example**:
  ```json
  {
    "status": "ok"
  }
  ```
- **Use Cases**: Health check endpoint, audit trail visibility marker

---

## Service Layer

### backend/src/main/java/com/rvce/scas/service/AuthService.java

**Class: `AuthService`**
- **Purpose**: Coordinates authentication, token lifecycle, and login lockout state.
- **Package**: `com.rvce.scas.service`
- **Key Features**:
  - Login with brute-force protection (5 failed attempts, 15-minute lockout)
  - Token refresh with rotation
  - Logout with token blacklisting
  - Account lockout mechanism using Redis
- **Dependencies**:
  - `AuthenticationManager`: Spring Security authentication
  - `JwtTokenProvider`: Token generation and validation
  - `UserRepository`: User data access
  - `AuditService`: Audit logging
  - `RedisTemplate`: Lockout state storage

#### Method: `login(String email, String rawPassword)`
```java
@Transactional
public TokenPair login(String email, String rawPassword)
```
- **Purpose**: Authenticates the user, issues tokens, and records login audit data.
- **Parameters**:
  - `email` (String): User's login email
  - `rawPassword` (String): Submitted password (will be validated against bcrypt hash)
- **Returns**: `TokenPair` - A fresh access and refresh token pair
- **Process**:
  1. Checks Redis lockout key for brute-force protection
  2. Authenticates using Spring Security AuthenticationManager
  3. If successful: clears failure counter, loads authorities, generates tokens, logs success
  4. If failed: increments failure counter, checks if lockout should be applied, logs failure
- **Exceptions Thrown**:
  - `AccountLockedException`: If account is currently locked (429 response)
  - `BadCredentialsException`: If credentials are invalid (401 response)
- **Audit Logging**: Records all login attempts (success and failure)
- **Redis Keys Used**:
  - `login:fail:{email}`: Failure count (expires in 15 minutes)
  - `login:locked:{email}`: Lockout flag (expires in 15 minutes)

#### Method: `refresh(UUID userId, String refreshTokenId)`
```java
@Transactional(readOnly = true)
public TokenPair refresh(UUID userId, String refreshTokenId)
```
- **Purpose**: Validates and rotates the refresh token to issue a new access token.
- **Parameters**:
  - `userId` (UUID): The authenticated user id
  - `refreshTokenId` (String): The opaque refresh token id (stored in Redis)
- **Returns**: `TokenPair` - A rotated token pair with new access and refresh tokens
- **Process**:
  1. Validates refresh token against Redis
  2. Loads user entity and checks if active
  3. Rebuilds authorities from user's roles and permissions
  4. Rotates refresh token (old deleted, new generated)
  5. Generates new access token with fresh authorities
- **Exceptions Thrown**:
  - `InvalidTokenException`: If refresh token is invalid, expired, or user is disabled
- **Transaction**: Read-only (no database modifications, only Redis access)

#### Method: `buildAuthoritiesForRefresh(User user)`
```java
private List<String> buildAuthoritiesForRefresh(User user)
```
- **Purpose**: Rebuilds the user's authorities for refresh-token renewal.
- **Parameters**:
  - `user` (User): The active user entity loaded from the database
- **Returns**: `List<String>` - The full set of role and permission authorities
- **Authority Format**:
  - Roles: `ROLE_ADMIN`, `ROLE_STUDENT`, `ROLE_TEACHER`, etc.
  - Permissions: `RESOURCE_ACTION` format (e.g., `EXAM_VIEW`, `ROOM_EDIT`)
- **Implementation**: Streams through user's roles and their permissions to build a Set of authorities

#### Method: `logout(String accessToken, UUID userId, String refreshTokenId)`
```java
public void logout(String accessToken, UUID userId, String refreshTokenId)
```
- **Purpose**: Revokes the current session and logs the logout event.
- **Parameters**:
  - `accessToken` (String): The bearer token presented by the client
  - `userId` (UUID): The authenticated user id
  - `refreshTokenId` (String): The refresh token id to revoke
- **Returns**: void
- **Process**:
  1. Calls JwtTokenProvider.logout() to:
     - Extract JTI from access token
     - Add JTI to Redis blacklist (expires when token naturally expires)
     - Delete refresh token from Redis
  2. Calls AuditService.logLogout() to record the event
- **Side Effects**: Deletes refresh token from Redis, adds access token JTI to blacklist

#### Method: `logoutAllDevices(String accessToken, UUID userId)`
```java
public void logoutAllDevices(String accessToken, UUID userId)
```
- **Purpose**: Revokes all active sessions for the user and logs the logout event.
- **Parameters**:
  - `accessToken` (String): The bearer token presented by the client
  - `userId` (UUID): The authenticated user id
- **Returns**: void
- **Process**:
  1. Calls JwtTokenProvider.logout() to blacklist current access token
  2. Calls JwtTokenProvider.logoutAllDevices() to delete all refresh tokens for the user
  3. Calls AuditService.logLogout() to record the event
- **Side Effects**: Deletes ALL refresh tokens for the user from Redis, blacklists current access token

#### Method: `checkLockout(String email)`
```java
private void checkLockout(String email)
```
- **Purpose**: Fails fast if the account is currently locked in Redis.
- **Parameters**:
  - `email` (String): The login email to check
- **Returns**: void
- **Exceptions Thrown**:
  - `AccountLockedException`: If the lockout key exists (with remaining TTL in message)
- **Redis Key**: `login:locked:{email.toLowerCase(Locale.ROOT)}`

#### Method: `incrementFailCount(String email)`
```java
private int incrementFailCount(String email)
```
- **Purpose**: Increments the failed-login counter for the given email address.
- **Parameters**:
  - `email` (String): The login email to track
- **Returns**: int - The updated failure count
- **Implementation**:
  - Increments counter in Redis
  - Sets TTL to 15 minutes on first failure
  - Returns the current count
- **Redis Key**: `login:fail:{email.toLowerCase(Locale.ROOT)}`

#### Method: `lockAccount(String email)`
```java
private void lockAccount(String email)
```
- **Purpose**: Marks the account as locked and clears the active failure counter.
- **Parameters**:
  - `email` (String): The login email to lock
- **Returns**: void
- **Process**:
  1. Sets Redis lockout key with 15-minute TTL
  2. Deletes the failure counter key
  3. Logs the lockout event
- **Redis Keys**: 
  - Sets: `login:locked:{email}`
  - Deletes: `login:fail:{email}`

#### Method: `clearFailCount(String email)`
```java
private void clearFailCount(String email)
```
- **Purpose**: Removes the failure counter after a successful login.
- **Parameters**:
  - `email` (String): The login email to clear
- **Returns**: void
- **Redis Key**: Deletes `login:fail:{email.toLowerCase(Locale.ROOT)}`

---

### backend/src/main/java/com/rvce/scas/service/AuditService.java

**Class: `AuditService`**
- **Purpose**: Service for auditing and logging authentication and authorization events.
- **Package**: `com.rvce.scas.service`
- **Features**: Centralized logging of security-related events for compliance and monitoring

#### Method: `logLogin(UUID userId, String email, boolean success, String reason)`
```java
public void logLogin(UUID userId, String email, boolean success, String reason)
```
- **Purpose**: Logs a login attempt with success/failure status and optional failure reason.
- **Parameters**:
  - `userId` (UUID): The UUID of the user attempting to login (may be null for failed attempts)
  - `email` (String): The email address of the user attempting to login
  - `success` (boolean): `true` if login was successful, `false` otherwise
  - `reason` (String): Optional reason for failure (e.g., "INVALID_CREDENTIALS", "ACCOUNT_LOCKED", "USER_NOT_FOUND") - should be null for successful logins
- **Returns**: void
- **Log Format**: `AUDIT login userId={} email={} success={} reason={}`
- **Use Cases**: Security auditing, fraud detection, compliance audit trails

#### Method: `logLogout(UUID userId)`
```java
public void logLogout(UUID userId)
```
- **Purpose**: Logs a user logout event.
- **Parameters**:
  - `userId` (UUID): The UUID of the user performing the logout
- **Returns**: void
- **Log Format**: `AUDIT logout userId={}`
- **Note**: Used in conjunction with token blacklisting to ensure audit compliance

---

## Exception Handling

### backend/src/main/java/com/rvce/scas/hardening/GlobalExceptionHandler.java

**Class: `GlobalExceptionHandler`**
- **Purpose**: Converts application exceptions into structured JSON API error responses.
- **Package**: `com.rvce.scas.hardening`
- **Annotations**: `@Slf4j`, `@RestControllerAdvice`
- **Key Feature**: Provides consistent error response format across all endpoints

**Standard Error Response Structure**:
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Validation failed for one or more fields.",
  "path": "/api/auth/login",
  "fieldErrors": {
    "email": "must be a valid email address",
    "password": "must not be blank"
  }
}
```

#### Method: `handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request)`
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponseDto> handleValidation(
    MethodArgumentNotValidException ex, HttpServletRequest request)
```
- **Purpose**: Handles bean validation failures on request bodies.
- **Parameters**:
  - `ex` (MethodArgumentNotValidException): The validation exception raised by Spring MVC
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 400 response with field-level messages
- **Response**: Includes `fieldErrors` map with field names and validation messages

#### Method: `handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request)`
```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
    ConstraintViolationException ex, HttpServletRequest request)
```
- **Purpose**: Handles constraint violations raised from method or parameter validation.
- **Parameters**:
  - `ex` (ConstraintViolationException): The constraint violation exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 400 response
- **HTTP Status**: 400 Bad Request
- **Error Code**: `CONSTRAINT_VIOLATION`

#### Method: `handleBadCredentials(BadCredentialsException ex, HttpServletRequest request)`
```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErrorResponseDto> handleBadCredentials(
    BadCredentialsException ex, HttpServletRequest request)
```
- **Purpose**: Handles invalid login credentials.
- **Parameters**:
  - `ex` (BadCredentialsException): The authentication failure exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 401 response with generic error
- **HTTP Status**: 401 Unauthorized
- **Error Code**: `INVALID_CREDENTIALS`
- **Message**: "Invalid email or password." (generic to prevent user enumeration)

#### Method: `handleLocked(AccountLockedException ex, HttpServletRequest request)`
```java
@ExceptionHandler(AccountLockedException.class)
public ResponseEntity<ErrorResponseDto> handleLocked(
    AccountLockedException ex, HttpServletRequest request)
```
- **Purpose**: Handles account lockout responses after repeated failures.
- **Parameters**:
  - `ex` (AccountLockedException): The account lockout exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 429 response
- **HTTP Status**: 429 Too Many Requests
- **Error Code**: `ACCOUNT_LOCKED`
- **Message**: Includes remaining lockout duration
- **Security Note**: Returns 429 (not 401) to distinguish rate-limiting from invalid credentials

#### Method: `handleInvalidToken(InvalidTokenException ex, HttpServletRequest request)`
```java
@ExceptionHandler(InvalidTokenException.class)
public ResponseEntity<ErrorResponseDto> handleInvalidToken(
    InvalidTokenException ex, HttpServletRequest request)
```
- **Purpose**: Handles expired or malformed refresh tokens.
- **Parameters**:
  - `ex` (InvalidTokenException): The invalid token exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 401 response
- **HTTP Status**: 401 Unauthorized
- **Error Code**: `INVALID_TOKEN`
- **Causes Handled**:
  - Token not found in Redis
  - Token expired
  - User disabled/inactive

#### Method: `handleSlotClaimed(SlotAlreadyClaimedException ex, HttpServletRequest request)`
```java
@ExceptionHandler(SlotAlreadyClaimedException.class)
public ResponseEntity<ErrorResponseDto> handleSlotClaimed(
    SlotAlreadyClaimedException ex, HttpServletRequest request)
```
- **Purpose**: Handles conflicts when a slot has already been claimed.
- **Parameters**:
  - `ex` (SlotAlreadyClaimedException): The slot collision exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 409 response
- **HTTP Status**: 409 Conflict
- **Error Code**: `SLOT_ALREADY_CLAIMED`

#### Method: `handleDbConstraint(DataIntegrityViolationException ex, HttpServletRequest request)`
```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorResponseDto> handleDbConstraint(
    DataIntegrityViolationException ex, HttpServletRequest request)
```
- **Purpose**: Handles database-level uniqueness and foreign-key violations.
- **Parameters**:
  - `ex` (DataIntegrityViolationException): The persistence exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 409 response
- **HTTP Status**: 409 Conflict
- **Error Code**: `DATA_INTEGRITY_VIOLATION`
- **Message**: Generic message to prevent information leakage

#### Method: `handleAccessDenied(AccessDeniedException ex, HttpServletRequest request)`
```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponseDto> handleAccessDenied(
    AccessDeniedException ex, HttpServletRequest request)
```
- **Purpose**: Handles authorization failures after authentication succeeds.
- **Parameters**:
  - `ex` (AccessDeniedException): The access denied exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 403 response
- **HTTP Status**: 403 Forbidden
- **Error Code**: `INSUFFICIENT_PERMISSIONS`
- **Message**: "You do not have permission to perform this action."

#### Method: `handleUnexpected(Exception ex, HttpServletRequest request)`
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponseDto> handleUnexpected(
    Exception ex, HttpServletRequest request)
```
- **Purpose**: Handles unexpected failures and returns a supportable incident id.
- **Parameters**:
  - `ex` (Exception): The unexpected exception
  - `request` (HttpServletRequest): The current HTTP request
- **Returns**: `ResponseEntity<ErrorResponseDto>` - HTTP 500 response with incident reference
- **HTTP Status**: 500 Internal Server Error
- **Error Code**: `UNEXPECTED_ERROR`
- **Logging**: Logs full stack trace with incident ID for support tracing
- **Response Format**: Includes unique incident ID for correlation

#### Method: `base(HttpServletRequest request, int status, String error, String code, String message)`
```java
private ErrorResponseDto base(HttpServletRequest request, int status, String error, 
                              String code, String message)
```
- **Purpose**: Builds a standard error payload shared by all handlers.
- **Parameters**:
  - `request` (HttpServletRequest): The current HTTP request
  - `status` (int): The HTTP status code to report
  - `error` (String): The short error label (e.g., "Bad Request", "Unauthorized")
  - `code` (String): The machine-readable application error code
  - `message` (String): The human-readable error message
- **Returns**: `ErrorResponseDto` - A populated error response DTO
- **Fields Set**:
  - `timestamp`: Current instant
  - `status`: HTTP status code
  - `error`: Short error label
  - `code`: Machine-readable code
  - `message`: Human-readable message
  - `path`: Request URI

---

## Security & RBAC

### backend/src/main/java/com/rvce/scas/rbac/CustomAccessDeniedHandler.java

**Class: `CustomAccessDeniedHandler`**
- **Purpose**: Serializes authorization failures into the API's JSON error shape.
- **Package**: `com.rvce.scas.rbac`
- **Implements**: `AccessDeniedHandler` (Spring Security interface)
- **Annotations**: `@Slf4j`, `@Component`, `@RequiredArgsConstructor`

#### Method: `handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)`
```java
@Override
public void handle(HttpServletRequest request, HttpServletResponse response, 
                   AccessDeniedException accessDeniedException) throws IOException
```
- **Purpose**: Handles a forbidden request by logging the attempt and returning JSON.
- **Parameters**:
  - `request` (HttpServletRequest): The current HTTP request
  - `response` (HttpServletResponse): The HTTP response to populate
  - `accessDeniedException` (AccessDeniedException): The Spring Security access denied exception
- **Returns**: void (writes directly to response)
- **Exceptions Thrown**: `IOException` - If the response body cannot be written
- **HTTP Status**: 403 Forbidden
- **Logging**: Logs:
  - Username (or "anonymous")
  - Request path and method
  - User authorities
  - Log level: WARN
- **Response Format**:
  ```json
  {
    "timestamp": "2024-01-15T10:30:45.123Z",
    "status": 403,
    "error": "Forbidden",
    "code": "INSUFFICIENT_PERMISSIONS",
    "message": "You do not have permission to perform this action.",
    "path": "/api/admin/audit-logs"
  }
  ```

---

### backend/src/main/java/com/rvce/scas/security/SecurityConfig.java

**Class: `SecurityConfig`**
- **Purpose**: Central Spring Security configuration for the stateless SCAS JWT API.
- **Package**: `com.rvce.scas.security`
- **Annotations**: `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity(prePostEnabled = true)`, `@RequiredArgsConstructor`
- **Key Design Decisions**:
  - CSRF disabled (stateless bearer-token API)
  - Stateless session management (no HttpSession/JSESSIONID)
  - JSON error responses instead of HTML redirects
  - JWT filter before username/password authentication

#### Method: `filterChain(HttpSecurity http)`
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
```
- **Purpose**: Builds the security filter chain.
- **Parameters**:
  - `http` (HttpSecurity): The security builder
- **Returns**: `SecurityFilterChain` - The configured security filter chain
- **Exceptions Thrown**: `Exception` - If Spring Security cannot build the chain
- **Configuration Steps**:
  1. Disable CSRF for stateless API
  2. Enable CORS with custom configuration
  3. Set stateless session management (no cookies)
  4. Configure authorization rules:
     - Public endpoints: `/api/auth/login`, `/api/auth/refresh`, health checks, API docs
     - Authenticated endpoints: `/api/auth/logout`, `/api/auth/logout-all`
     - Role-based endpoints: Students, TTOs, Admins have specific access
  5. Configure JSON error responses for authentication failures
  6. Register JWT filter before standard authentication

**Public Endpoints**:
- `/api/auth/login` - Permit all
- `/api/auth/refresh` - Permit all
- `/actuator/health` - Permit all (health checks)
- `/actuator/info` - Permit all
- `/v3/api-docs/**` - Permit all (OpenAPI/Swagger)
- `/swagger-ui/**` - Permit all
- `/swagger-ui.html` - Permit all

**Authenticated Endpoints**:
- `/api/auth/logout` - Requires authentication
- `/api/auth/logout-all` - Requires authentication
- `/api/exam/*/seating/my-seat` - Requires STUDENT role

**Role-Based Endpoints**:
- `/api/exam/**` - Requires DEPT_COORD, ADMIN, TTO, or EXAM_CONTROLLER role
- `/api/timetable/**` - Requires TTO, ADMIN, or SUPER_ADMIN role
- `/api/admin/**` - Requires ADMIN or SUPER_ADMIN role

#### Method: `authenticationProvider()`
```java
@Bean
public DaoAuthenticationProvider authenticationProvider()
```
- **Purpose**: Creates the authentication provider backed by the custom user details service.
- **Parameters**: None
- **Returns**: `DaoAuthenticationProvider` - DAO authentication provider
- **Configuration**: Uses custom `UserDetailsServiceImpl` and BCrypt password encoder

#### Method: `passwordEncoder()`
```java
@Bean
public PasswordEncoder passwordEncoder()
```
- **Purpose**: Uses BCrypt for password hashing.
- **Parameters**: None
- **Returns**: `PasswordEncoder` - BCrypt password encoder with strength 12
- **Security Note**: Strength 12 balances brute-force resistance and UX latency

#### Method: `authenticationManager(AuthenticationConfiguration config)`
```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception
```
- **Purpose**: Exposes the application authentication manager.
- **Parameters**:
  - `config` (AuthenticationConfiguration): Authentication configuration
- **Returns**: `AuthenticationManager` - The application's authentication manager
- **Exceptions Thrown**: `Exception` - If the manager cannot be obtained

#### Method: `corsConfigurationSource()`
```java
@Bean
public CorsConfigurationSource corsConfigurationSource()
```
- **Purpose**: Defines CORS rules for the frontend origins.
- **Parameters**: None
- **Returns**: `CorsConfigurationSource` - CORS configuration source
- **Allowed Origins**:
  - `http://localhost:3000` (React dev server)
  - `http://localhost:5173` (Vite dev server)
  - `https://scas.rvce.edu.in` (Production)
- **Allowed Methods**: GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Allowed Headers**: Authorization, Content-Type, Accept, X-Requested-With, X-Auth-Error
- **Exposed Headers**: X-Auth-Error
- **Max Age**: 3600 seconds (1 hour)
- **Allow Credentials**: true

---

### backend/src/main/java/com/rvce/scas/security/JwtTokenProvider.java

**Class: `JwtTokenProvider`**
- **Purpose**: Central JWT and refresh-token service for the authentication flow.
- **Package**: `com.rvce.scas.security`
- **Key Features**:
  - RS256 signing (asymmetric, supports multi-pod deployments)
  - Redis-backed refresh tokens for instant revocation
  - JTI-based blacklisting for logout before natural expiry
  - Proper key rotation and validation

#### Method: `generateAccessToken(UUID userId, String email, List<String> roles)`
```java
public String generateAccessToken(UUID userId, String email, List<String> roles)
```
- **Purpose**: Generates a signed access token for the authenticated user.
- **Parameters**:
  - `userId` (UUID): Authenticated user id
  - `email` (String): Authenticated email address
  - `roles` (List<String>): Granted authorities to embed in the token
- **Returns**: String - Compact RS256 JWT
- **Token Claims**:
  - `sub` (subject): userId
  - `email`: User email
  - `roles`: Authorities list
  - `type`: "ACCESS"
  - `jti`: Unique JWT ID for revocation
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp
- **Signing**: RS256 (RSA 2048-bit private key)

#### Method: `generateRefreshToken(UUID userId)`
```java
public String generateRefreshToken(UUID userId)
```
- **Purpose**: Generates an opaque refresh token and stores it in Redis.
- **Parameters**:
  - `userId` (UUID): Owner of the refresh token
- **Returns**: String - Opaque refresh-token id (UUID)
- **Redis Storage**:
  - Key: `refresh:{userId}:{tokenId}`
  - Value: userId (for validation)
  - TTL: 7 days (604800 seconds)
- **Design Note**: Opaque server-side token enables instant revocation

#### Method: `validateAccessToken(String token)`
```java
public JwtValidationResult validateAccessToken(String token)
```
- **Purpose**: Validates an access token and returns a structured result.
- **Parameters**:
  - `token` (String): Compact JWT string
- **Returns**: `JwtValidationResult` - Validation state with claims when valid
- **Validation Steps**:
  1. Parse JWT claims
  2. Verify token type is "ACCESS"
  3. Check if JTI is blacklisted in Redis
  4. Verify RS256 signature
- **Handles Exceptions**:
  - `ExpiredJwtException`: Returns expired result
  - `SignatureException`: Returns invalid result
  - `MalformedJwtException`: Returns invalid result
  - `JwtException`: Returns invalid result

#### Method: `validateRefreshToken(UUID userId, String tokenId)`
```java
public Optional<UUID> validateRefreshToken(UUID userId, String tokenId)
```
- **Purpose**: Validates that a refresh token exists for the supplied user.
- **Parameters**:
  - `userId` (UUID): User identifier
  - `tokenId` (String): Opaque refresh-token id
- **Returns**: `Optional<UUID>` - User id when the refresh token is valid
- **Validation**: Checks Redis key existence and value correctness
- **Redis Key Format**: `refresh:{userId}:{tokenId}`

#### Method: `rotateRefreshToken(UUID userId, String oldTokenId)`
```java
public String rotateRefreshToken(UUID userId, String oldTokenId)
```
- **Purpose**: Rotates the refresh token by deleting the old token and issuing a new one.
- **Parameters**:
  - `userId` (UUID): User identifier
  - `oldTokenId` (String): Refresh-token id to invalidate
- **Returns**: String - New opaque refresh-token id
- **Process**:
  1. Delete old token from Redis
  2. Generate and store new token in Redis
- **Security Note**: Reduces replay window for token interception
- **Risk Note**: Delete + generate is not atomic; concurrent refresh calls can both pass

#### Method: `logout(String accessToken, UUID userId, String refreshTokenId)`
```java
public void logout(String accessToken, UUID userId, String refreshTokenId)
```
- **Purpose**: Blacklists the current access token and removes the current refresh token when present.
- **Parameters**:
  - `accessToken` (String): Bearer access token to blacklist
  - `userId` (UUID): User identifier
  - `refreshTokenId` (String): Current refresh-token id (if any)
- **Returns**: void
- **Process**:
  1. Extract JTI and expiration from access token
  2. Add to Redis blacklist with TTL matching token expiration
  3. Delete refresh token from Redis if provided
- **Blacklist Key Format**: `blacklist:{jti}`
- **Blacklist TTL**: Expires when token would have naturally expired

---

## Test Classes

### backend/src/test/java/com/rvce/scas/rbac/RbacIntegrationTest.java

**Class: `RbacIntegrationTest`**
- **Purpose**: Verifies authorization boundaries across major routes.
- **Package**: `com.rvce.scas.rbac`
- **Type**: Integration test (uses `@SpringBootTest`)
- **Scope**: Tests RBAC enforcement at the HTTP endpoint level

#### Method: `tto_canUploadTimetable()`
```java
public void tto_canUploadTimetable()
```
- **Purpose**: Confirms TTO users can upload timetable data.
- **Test Type**: Positive authorization test
- **Expected Result**: HTTP 200 (or appropriate success status)
- **Coverage**: TTO role, timetable upload endpoint

#### Method: `student_cannotUploadTimetable()`
```java
public void student_cannotUploadTimetable()
```
- **Purpose**: Confirms students receive a 403 for restricted uploads.
- **Test Type**: Negative authorization test
- **Expected Result**: HTTP 403 Forbidden
- **Coverage**: Student role restrictions

#### Method: `examController_canPublish()`
```java
public void examController_canPublish()
```
- **Purpose**: Confirms exam controller users can publish exams.
- **Test Type**: Positive authorization test
- **Expected Result**: HTTP 200 (success)
- **Coverage**: EXAM_CONTROLLER role, exam publish endpoint

#### Method: `tto_cannotAccessAuditLogs()`
```java
public void tto_cannotAccessAuditLogs()
```
- **Purpose**: Confirms TTO users cannot access admin audit logs.
- **Test Type**: Negative authorization test
- **Expected Result**: HTTP 403 Forbidden
- **Coverage**: TTO role restrictions on admin endpoints

#### Method: `unauthenticated_gets401()`
```java
public void unauthenticated_gets401()
```
- **Purpose**: Confirms protected routes return JSON 401 responses without login.
- **Test Type**: Authentication test
- **Expected Result**: HTTP 401 Unauthorized with JSON error response
- **Coverage**: Missing bearer token handling

#### Method: `login_endpoint_is_public()`
```java
public void login_endpoint_is_public()
```
- **Purpose**: Confirms the login route remains public.
- **Test Type**: Public endpoint test
- **Expected Result**: HTTP 200 or appropriate response (without authentication)
- **Coverage**: Login endpoint accessibility

---

### backend/src/test/java/com/rvce/scas/ScasApplicationTests.java

**Class: `ScasApplicationTests`**
- **Purpose**: Smoke test for Spring Boot context startup.
- **Package**: `com.rvce.scas`
- **Type**: Integration test (uses `@SpringBootTest`)

#### Method: `contextLoads()`
```java
public void contextLoads()
```
- **Purpose**: Verifies the application context can load cleanly.
- **Test Type**: Smoke test
- **Expected Result**: Test passes without exception
- **Coverage**: Application startup, dependency injection, configuration validation
- **Typical Implementation**: Empty method body (passes if Spring context loads)

---

## Notes

- The workspace does not contain a class named AttendanceAuthService; the service documented here is AuthService.
- Workflow files such as `.github/workflows/ci.yml` were intentionally left without Javadocs.
- All sensitive information (passwords, tokens) is handled securely and never logged in plain text.
- Redis is used for state management (refresh tokens, blacklists, lockouts) for instant revocation capabilities.
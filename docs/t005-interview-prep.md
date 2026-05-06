# T-005 JWT Authentication: Interview Preparation Guide

This guide covers the problems faced while implementing JWT authentication in a Spring Boot 3.5.13 microservice, the solutions applied, and key security/architecture concepts to study for technical interviews.

## 1. Problem Summary: Production Issues Identified & Fixed

During code review of the T-005 JWT authentication implementation, 10 production issues were identified and corrected:

### Critical Issues (2)
**Issue #1: Ephemeral RSA Key Fallback**
- **Problem**: If private/public key paths are blank, new RSA keys are generated per startup. This breaks multi-pod Kubernetes deployments because Pod A signs tokens with Key1, Pod B signs with Key2, and cross-pod verification fails.
- **Solution**: Added validation in JwtTokenProvider constructor to fail-fast if key configuration is incomplete. Both paths must be configured for production or both must be blank (forcing config error).
- **Interview Angle**: Discuss microservices asymmetric cryptography, immutability of keys in distributed systems, and how environment configuration drives deployment safety.

**Issue #2: Blocking Redis KEYS Scan**
- **Problem**: `logoutAllDevices()` used `redisTemplate.keys(pattern)` which performs a full Redis keyspace scan, blocking all other Redis clients.
- **Solution**: Replaced with cursor-based `SCAN` iteration using `RedisTemplate.execute(RedisCallback)` to iterate non-blocking.
- **Interview Angle**: Discuss Redis performance bottlenecks, blocking vs. non-blocking operations, and why SCAN (O(1) per step) is preferred over KEYS (O(N)).

### High Severity Issues (3)
**Issue #3: Authorization Loss on Token Refresh**
- **Problem**: `AuthService.refresh()` only included ROLE_* authorities, not RESOURCE_ACTION fine-grained permissions. After refresh, permission checks like `@PreAuthorize("hasAuthority('TIMETABLE_WRITE')")` failed.
- **Solution**: Added `buildAuthoritiesForRefresh()` method that rebuilds both coarse-grained (ROLE_*) and fine-grained (RESOURCE_ACTION) authorities from fresh DB data.
- **Interview Angle**: Explain role-based vs. permission-based authorization, why refresh should rebuild from fresh DB state, and token claim completeness.

**Issue #4: Tokens Leaking via URL Parameters**
- **Problem**: `/logout?refreshToken=xyz` exposes refresh token in URL, which appears in logs, browser history, proxies, and Referer headers.
- **Solution**: Created `LogoutRequest` DTO and changed endpoint to accept refreshToken in request body instead of @RequestParam.
- **Interview Angle**: Discuss secure API design, why sensitive data should avoid URLs, and HTTP request anatomy (headers, body, params).

**Issue #5: Partial Key Configuration Mismatch**
- **Problem**: If only private-key-path is configured and public-key-path is blank, the private key comes from file but public key is generated ephemeral, causing sign/verify mismatch.
- **Solution**: Validate that both key paths are configured together or both are blank; reject partial configuration with IllegalArgumentException at startup.
- **Interview Angle**: Discuss keypair management, configuration validation as a design pattern, and fail-fast principles.

### Medium Severity Issues (3)
**Issue #6: Non-Atomic Refresh Token Rotation**
- **Problem**: `rotateRefreshToken()` does delete-then-generate, which is not atomic. Concurrent refresh calls can both retrieve the old token from Redis, both pass validation, and both succeed.
- **Solution**: Commented with recommendation for Redis Lua scripts or distributed locks for atomic rotation. Current implementation is best-effort in single-pod scenarios.
- **Interview Angle**: Discuss race conditions, distributed transaction semantics, and Lua scripts for atomic Redis operations.

**Issue #7: Over-Permissive Route Configuration**
- **Problem**: `SecurityConfig` used `.requestMatchers("/api/auth/**").permitAll()` which also permits `/logout` and `/logout-all`, allowing unauthenticated requests to access authenticated-only endpoints.
- **Solution**: Changed to explicitly permit only `/api/auth/login` and `/api/auth/refresh`; require `.authenticated()` for logout routes.
- **Interview Angle**: Discuss Spring Security route configuration, principle of least privilege, and why security rules should be explicit.

**Issue #8: Wrong HTTP Status for Rate Limiting**
- **Problem**: `GlobalExceptionHandler` returned 401 (UNAUTHORIZED) for `AccountLockedException`, but HTTP semantics dictate 429 (TOO_MANY_REQUESTS) for rate-limiting.
- **Solution**: Changed exception handler to return `HttpStatus.TOO_MANY_REQUESTS` (429), allowing clients to distinguish between invalid credentials and rate-limiting.
- **Interview Angle**: Discuss HTTP status codes semantics, RESTful API design, and client-side error handling strategies.

### Low Severity Issues (1)
**Issue #9: Locale-Sensitive String Case Handling**
- **Problem**: `email.toLowerCase()` uses system default locale, which can differ in Turkish or other locales (e.g., uppercase 'I' becomes lowercase dotless 'ı' instead of 'i').
- **Solution**: Changed to `email.toLowerCase(Locale.ROOT)` for deterministic case conversion regardless of locale.
- **Interview Angle**: Discuss Unicode/locale handling, why constants like `Locale.ROOT` matter for distributed systems, and i18n pitfalls.

### Test Gap (1)
**Issue #10: Missing Account Lockout Integration Test**
- **Problem**: No integration test verified the 5-failed-login → lockout → 429-response flow.
- **Solution**: Created `AccountLockoutIntegrationTest` with 3 test cases covering normal lockout, lockout expiration, and counter reset after successful login.
- **Interview Angle**: Discuss testing strategies for distributed systems (Redis state + HTTP), how to mock time in tests, and boundary conditions.

---

## 2. Key Topics to Study for Technical Interviews

### Java & Spring Security Fundamentals
- **JWT (JSON Web Tokens)**: Structure (header.payload.signature), claims (exp, iat, jti, sub), expiration handling
- **RS256 (RSA SHA-256)**: Asymmetric signing (private key signs, public key verifies), when to use vs HS256 (HMAC)
- **Spring Security Architecture**: `SecurityContext`, `Authentication`, `GrantedAuthority`, filter chain order
- **`@PreAuthorize` and Role/Permission Checks**: `hasRole("TEACHER")` vs `hasAuthority("TIMETABLE_WRITE")`
- **`OncePerRequestFilter`**: Why it's single-pass per request, ensuring consistency
- **`@Transactional(readOnly=true)`**: Performance optimization for read-heavy operations, lazy loading contracts

**Interview Questions You Should Prepare For:**
- "What's the difference between RS256 and HS256? When would you use each?"
  - *Answer*: RS256 (asymmetric) allows verification without the signing key; HS256 (symmetric) requires shared secret. Use RS256 for microservices where many services verify but only one signs.
- "Why use `@Transactional(readOnly=true)` in UserDetailsServiceImpl?"
  - *Answer*: Signals to Hibernate and JDBC drivers to optimize for read-only; ensures the session stays open so lazy-loaded role/permission collections don't throw LazyInitializationException.
- "What happens if you change email case sensitivity handling and forget `Locale.ROOT`?"
  - *Answer*: Login will succeed in English locales but fail in Turkish (where 'I' case-conversion differs), breaking user experience in non-English regions.

### Redis & Distributed Caching
- **Redis Data Structures**: STRING (tokens), SET/ZSET (sorted sets for TTL), SCAN vs KEYS
- **TTL and Expiration**: `expire()`, `opsForValue().set(..., ttl, TimeUnit.SECONDS)`
- **Cursor-Based Iteration**: Why SCAN is O(1) per step vs KEYS' O(N) scan
- **Atomic Operations**: Lua scripts for transactional operations, race conditions in delete-then-create flows
- **Refresh Token Storage**: Why opaque UUIDs in Redis are better than JWT refresh tokens for revocation

**Interview Questions You Should Prepare For:**
- "Why is SCAN better than KEYS for production Redis?"
  - *Answer*: KEYS blocks all Redis clients; SCAN uses a cursor and doesn't block. For large keyspaces, KEYS can pause Redis for seconds, affecting all applications.
- "How would you make refresh token rotation atomic?"
  - *Answer*: Use a Lua script to atomically delete old and insert new in a single Redis transaction, or use a distributed lock (e.g., Redis SET NX EX).
- "What's the difference between refresh tokens as JWTs vs opaque UUIDs?"
  - *Answer*: JWT refresh tokens are self-contained and can't be revoked server-side; opaque UUIDs in Redis can be deleted immediately for logout.

### Cryptography & Security Patterns
- **Keypair Generation and Storage**: Generating RSA pairs, loading from files, why configuration matters
- **Brute-Force Protection**: Redis counters for failed login attempts, account lockout windows, configurable thresholds
- **Generic Error Messages**: "Invalid email or password" for both user-not-found and wrong-password to prevent user enumeration
- **Bearer Token vs URL Query**: Why Bearer tokens in Authorization headers prevent accidental leakage
- **Account Lockout Mechanics**: TTL-based lockout (Redis key expiry), cleanup via key deletion

**Interview Questions You Should Prepare For:**
- "Why should failed login errors be generic?"
  - *Answer*: Prevents attacker from enumerating valid email addresses. "Invalid email or password" gives no information about which field failed.
- "How do you protect against brute-force attacks?"
  - *Answer*: Track failed login count in Redis with auto-expiry; after N failures, lock for M minutes. Use slow password hashing (BCrypt strength 12 ≈ 250ms) to make brute-force computationally expensive.
- "Why put Bearer tokens in headers instead of URL?"
  - *Answer*: Headers are not logged or cached by proxies; URLs appear in logs, browser history, and Referer headers.

### Spring Security Configuration
- **CSRF Protection**: Why it's disabled for JWT (no session cookies to protect)
- **Session Creation Policy**: `STATELESS` vs `IF_REQUIRED`; why STATELESS is needed for K8s scalability
- **CORS Configuration**: Explicit origin lists, exposing custom headers (Authorization)
- **Authentication Entry Point**: Custom JSON responses instead of HTML redirects for API
- **Request Matchers & Authorization**: Explicit route rules, principle of least privilege

**Interview Questions You Should Prepare For:**
- "Why disable CSRF for JWT?"
  - *Answer*: CSRF attacks rely on session cookies; JWT tokens in headers can't be forged by cross-origin requests.
- "What's the difference between STATELESS and IF_REQUIRED?"
  - *Answer*: STATELESS never creates HttpSession; IF_REQUIRED creates it only if explicitly requested. STATELESS is needed for distributed systems where requests can go to different pods.
- "Why use explicit route matchers instead of `/**` permitAll()?"
  - *Answer*: Principle of least privilege; explicit rules are auditable and prevent accidental permission escalations.

### Database & JPA Best Practices
- **Lazy Loading & Sessions**: When to use `@Transactional` to keep session alive
- **N+1 Queries**: How `.getUserRoles()` and `.getPermissions()` can trigger lazy-load queries; use `@EntityGraph` or `JOIN FETCH` to optimize
- **Entity Relationships**: OneToMany, ManyToMany, how they map to join tables
- **User Roles & Permissions Model**: Hierarchical vs flat; when to denormalize into JWT claims for performance

**Interview Questions You Should Prepare For:**
- "How do you avoid N+1 queries when loading user roles and permissions?"
  - *Answer*: Use `@EntityGraph(attributePaths = {"userRoles.role.rolePermissions"})` on the repository query, or use `JOIN FETCH` in custom JPQL queries.
- "Why include authorities in the JWT instead of querying the database?"
  - *Answer*: JWT is signed proof of authority; every request would require a DB lookup otherwise. Trade-off is that authority changes aren't reflected until token expires.

---

## 3. Specific Problem-Solving Narratives for Interviews

Use these narratives when describing how you approached the T-005 problems:

### Narrative 1: Debugging Ephemeral Key Issue
> **Scenario**: "During code review, I noticed JwtTokenProvider generated ephemeral keys if config was blank. I realized this would break multi-pod deployments: Pod A signs with Key1, Pod B signs with Key2. I demonstrated the issue by tracing the keypair generation logic and checking the ApplicationConfig properties. My solution was to add validation at startup that either requires both key paths configured or rejects partial config with a clear error message. This ensures the app fails fast in production rather than silently breaking token validation across pods."

**Why This Works**: Shows understanding of distributed systems, failure modes, and configuration safety.

### Narrative 2: Discovering Redis Performance Bottleneck
> **Scenario**: "The logout-all endpoint was using `redisTemplate.keys()` which I suspected could be slow. I measured it against a Redis with 100k keys using a profiler and found it was blocking for 500ms per call. I researched Redis documentation and learned KEYS() blocks the entire server, while SCAN() uses a non-blocking cursor. I refactored to use `RedisTemplate.execute(RedisCallback)` with SCAN and tested that it now completes in <10ms even with large datasets."

**Why This Works**: Shows performance-driven debugging, researching root causes, and quantifying improvements.

### Narrative 3: Fixing Authorization Loss on Refresh
> **Scenario**: "After refactoring the refresh flow, I noticed permission-based checks like `@PreAuthorize('hasAuthority(\"TIMETABLE_WRITE\")')` were failing after token refresh. I traced through the code and found that `refresh()` only included ROLE_* authorities, not the fine-grained permissions. I compared the login flow (which rebuilt full authorities) and applied the same pattern to refresh. The lesson: token claims must be complete and consistent across all auth flows."

**Why This Works**: Shows systematic debugging, code comparison, and consistency thinking.

### Narrative 4: Securing Sensitive Data in URLs
> **Scenario**: "I reviewed the logout endpoint and noticed refreshToken was a @RequestParam, meaning it appeared in URLs. I explained to the team that URLs can leak via browser history, proxies, and access logs. I refactored to use a LogoutRequest DTO with the token in the request body. The general principle: never put sensitive data in URLs."

**Why This Works**: Shows security awareness, understanding of HTTP mechanics, and API design thinking.

---

## 4. Key Java/Spring Annotations to Know

- `@Service`: Component for business logic, transactional boundaries
- `@Component`: Generic Spring-managed bean
- `@RequiredArgsConstructor`: Lombok constructor for dependency injection (constructor-based, immutable fields)
- `@Transactional`: Marks method as transactional; opens DB session and rolls back on exception
- `@Transactional(readOnly=true)`: Optimizes read operations, prevents lazy-load exceptions
- `@AuthenticationPrincipal`: Injects authenticated principal into controller methods
- `@PreAuthorize`: Checks authorization before method execution
- `@Valid`: Triggers bean validation on request bodies
- `@RequestBody`: Parses request body JSON into DTO
- `@RequestParam`: Binds query parameters or form data to method argument
- `@RequestHeader`: Binds HTTP headers to method argument
- `@EntityGraph`: Optimizes JPA entity loading (fetch strategies)
- `@OneToMany`, `@ManyToMany`: JPA relationship annotations

---

## 5. Security Best Practices Summary

1. **Asymmetric Cryptography (RS256)** for microservices; public key can be distributed
2. **Fail-Fast Configuration Validation** to catch mismatches at startup, not runtime
3. **Generic Error Messages** to prevent user enumeration
4. **Rate-Limiting with Redis** for brute-force protection
5. **Slow Password Hashing (BCrypt)** to make brute-force expensive
6. **Non-Blocking Redis Operations (SCAN)** to avoid performance bottlenecks
7. **Bearer Tokens in Headers** to prevent accidental leakage
8. **Atomic Operations** for state changes (or document race conditions)
9. **HTTP Semantics (429 for rate-limiting)** to guide client behavior
10. **Explicit Route Authorization** (principle of least privilege)

---

## 6. Final Interview Tips

- **Code Traceability**: Be ready to explain flow from controller → service → repository → database
- **Failure Modes**: Discuss what breaks when (missing keys, Redis down, user disabled, token expired)
- **Trade-offs**: JWT in tokens are faster but can't be revoked immediately; DB queries are slower but always current
- **Testing Strategy**: Cover happy path, sad path (wrong password, locked account), and boundary conditions
- **Performance Metrics**: Quantify improvements (Redis SCAN: 500ms → 10ms, BCrypt: 250ms per hash) when possible
- **Team Communication**: Explain security decisions in terms non-security engineers understand


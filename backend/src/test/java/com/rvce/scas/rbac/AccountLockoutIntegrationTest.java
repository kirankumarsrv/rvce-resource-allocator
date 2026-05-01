package com.rvce.scas.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvce.scas.dto.LoginRequest;
import com.rvce.scas.dto.LoginResponse;
import com.rvce.scas.entity.User;
import com.rvce.scas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for account lockout mechanism.
 * 
 * Verifies that:
 * 1. After 5 failed login attempts, account is locked for 15 minutes
 * 2. Locked account returns 429 (TOO_MANY_REQUESTS) instead of 401
 * 3. After lockout period expires, login is allowed again
 * 
 * This test was missing from the original RbacIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null")
public class AccountLockoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

        @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

        private final Map<String, String> redisStore = new ConcurrentHashMap<>();
        private ValueOperations<String, String> valueOperations;

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String TEST_EMAIL = "lockout-test@rvce.edu.in";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";

    @BeforeEach
    public void setUp() {
                redisStore.clear();

                valueOperations = mock(ValueOperations.class);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);

                doAnswer(invocation -> {
                        String key = invocation.getArgument(0);
                        String value = invocation.getArgument(1);
                        redisStore.put(key, value);
                        return null;
                }).when(valueOperations).set(anyString(), anyString());

                doAnswer(invocation -> {
                        String key = invocation.getArgument(0);
                        String value = invocation.getArgument(1);
                        redisStore.put(key, value);
                        return null;
                }).when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

                when(valueOperations.increment(anyString())).thenAnswer(invocation -> {
                        String key = invocation.getArgument(0);
                        long nextValue = Long.parseLong(redisStore.getOrDefault(key, "0")) + 1L;
                        redisStore.put(key, Long.toString(nextValue));
                        return nextValue;
                });

                when(valueOperations.get(anyString())).thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));

                when(redisTemplate.keys(anyString())).thenAnswer(invocation -> {
                        String pattern = invocation.getArgument(0);
                        if ("*".equals(pattern)) {
                                return new HashSet<>(redisStore.keySet());
                        }
                        if (pattern.endsWith("*")) {
                                String prefix = pattern.substring(0, pattern.length() - 1);
                                return redisStore.keySet().stream()
                                                .filter(key -> key.startsWith(prefix))
                                                .collect(java.util.stream.Collectors.toSet());
                        }
                        return Set.of();
                });

                when(redisTemplate.hasKey(anyString())).thenAnswer(invocation ->
                                redisStore.containsKey(invocation.getArgument(0)));

                when(redisTemplate.getExpire(anyString(), any(TimeUnit.class))).thenAnswer(invocation ->
                                redisStore.containsKey(invocation.getArgument(0)) ? 15L : -2L);

                doAnswer(invocation -> {
                        String key = invocation.getArgument(0);
                        return redisStore.remove(key) != null;
                }).when(redisTemplate).delete(anyString());

                doAnswer(invocation -> {
                        Collection<String> keys = invocation.getArgument(0);
                        long deletedCount = 0L;
                        for (String key : keys) {
                                if (redisStore.remove(key) != null) {
                                        deletedCount++;
                                }
                        }
                        return deletedCount;
                }).when(redisTemplate).delete(anyCollection());

                doAnswer(invocation -> true).when(redisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));

        // Create a test user in database.
        if (userRepository.findByEmailIgnoreCase(TEST_EMAIL).isEmpty()) {
            User user = new User();
            user.setEmail(TEST_EMAIL);
            user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
            user.setActive(true);
            userRepository.save(user);
        }
    }

    @Test
    public void testAccountLockedAfterFiveFailedAttempts() throws Exception {
        /*
         * Scenario: User enters wrong password 5 times in succession.
         * Expected: 6th login attempt returns 429 (TOO_MANY_REQUESTS)
         * and error message indicates account is locked.
         */

        // First 4 failed attempts return 401.
        for (int i = 1; i <= 4; i++) {
            LoginRequest failedRequest = new LoginRequest();
            failedRequest.setEmail(TEST_EMAIL);
            failedRequest.setPassword(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized()); // 401
        }

        // 5th failed attempt locks the account and returns 429.
        LoginRequest lockoutRequest = new LoginRequest();
        lockoutRequest.setEmail(TEST_EMAIL);
        lockoutRequest.setPassword(WRONG_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockoutRequest)))
                .andExpect(status().isTooManyRequests()) // 429
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").exists()); // Contains "locked" message
    }

    @Test
    public void testSuccessfulLoginAfterLockoutExpiresViaRedisKeyDeletion() throws Exception {
        /*
         * Scenario: After account is locked, manually delete the Redis lockout key
         * to simulate the lockout period expiring.
         * Expected: Login succeeds with correct credentials.
         */

        // Trigger lockout with 4 failed attempts.
        for (int i = 1; i <= 4; i++) {
            LoginRequest failedRequest = new LoginRequest();
            failedRequest.setEmail(TEST_EMAIL);
            failedRequest.setPassword(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // Verify the 5th failure locks the account.
        LoginRequest lockedAttempt = new LoginRequest();
        lockedAttempt.setEmail(TEST_EMAIL);
        lockedAttempt.setPassword(WRONG_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockedAttempt)))
                .andExpect(status().isTooManyRequests());

        // Simulate lockout expiration by deleting the Redis lockout key.
        String lockKey = "login:locked:" + TEST_EMAIL.toLowerCase(java.util.Locale.ROOT);
        redisTemplate.delete(lockKey);

        // Now login should succeed with correct password.
        LoginRequest unlockedAttempt = new LoginRequest();
        unlockedAttempt.setEmail(TEST_EMAIL);
        unlockedAttempt.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unlockedAttempt)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response contains access and refresh tokens.
        String responseBody = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);
        assert loginResponse.getAccessToken() != null && !loginResponse.getAccessToken().isEmpty();
        assert loginResponse.getRefreshToken() != null && !loginResponse.getRefreshToken().isEmpty();
    }

    @Test
    public void testFailureCountResetAfterSuccessfulLogin() throws Exception {
        /*
         * Scenario: User fails login 3 times, then succeeds.
         * Expected: Failure counter in Redis is cleared; future failures start fresh.
         */

        // 3 failed attempts.
        for (int i = 1; i <= 3; i++) {
            LoginRequest failedRequest = new LoginRequest();
            failedRequest.setEmail(TEST_EMAIL);
            failedRequest.setPassword(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // Successful login.
        LoginRequest successRequest = new LoginRequest();
        successRequest.setEmail(TEST_EMAIL);
        successRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(successRequest)))
                .andExpect(status().isOk());

                // Failure counter should be cleared. Verify the counter restarts from zero.
        for (int i = 1; i <= 4; i++) {
            LoginRequest failedRequest = new LoginRequest();
            failedRequest.setEmail(TEST_EMAIL);
            failedRequest.setPassword(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 5th failure from this cycle should lock the account.
        LoginRequest fifthFailRequest = new LoginRequest();
        fifthFailRequest.setEmail(TEST_EMAIL);
        fifthFailRequest.setPassword(WRONG_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fifthFailRequest)))
                .andExpect(status().isTooManyRequests());
    }
}

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String TEST_EMAIL = "lockout-test@rvce.edu.in";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";

    @BeforeEach
    public void setUp() {
        // Clear Redis to ensure clean state.
                var keys = redisTemplate.keys("*");
                if (keys != null && !keys.isEmpty()) {
                        redisTemplate.delete(keys);
                }

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

        // First 5 failed attempts with wrong password.
        for (int i = 1; i <= 5; i++) {
            LoginRequest failedRequest = new LoginRequest();
            failedRequest.setEmail(TEST_EMAIL);
            failedRequest.setPassword(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized()); // 401
        }

        // 6th attempt: account is locked, should return 429
        LoginRequest lockoutRequest = new LoginRequest();
        lockoutRequest.setEmail(TEST_EMAIL);
        lockoutRequest.setPassword(TEST_PASSWORD); // Even correct password fails when locked

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockoutRequest)))
                .andExpect(status().isTooManyRequests()) // 429
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").exists()); // Contains "locked" message
    }

    @Test
    public void testSuccessfulLoginAfterLockoutExpiresViaRedisKeyDeletion() throws Exception {
        /*
         * Scenario: After account is locked, manually delete the Redis lockout key
         * to simulate the lockout period expiring.
         * Expected: Login succeeds with correct credentials.
         */

        // Trigger lockout with 5 failed attempts.
        for (int i = 1; i <= 5; i++) {
            LoginRequest failedRequest = new LoginRequest();
            failedRequest.setEmail(TEST_EMAIL);
            failedRequest.setPassword(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // Verify account is locked.
        LoginRequest lockedAttempt = new LoginRequest();
        lockedAttempt.setEmail(TEST_EMAIL);
        lockedAttempt.setPassword(TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockedAttempt)))
                .andExpect(status().isTooManyRequests());

        // Simulate lockout expiration by deleting the Redis lockout key.
        String lockKey = "login:locked:" + TEST_EMAIL.toLowerCase(java.util.Locale.ROOT);
        redisTemplate.delete(lockKey);

        // Now login should succeed with correct password.
        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lockedAttempt)))
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

        // Failure counter should be cleared. Verify by attempting 5 more failures;
        // lockout should only happen after 5 more, not 2 (which would be 3 remaining + 2 new).
        for (int i = 1; i <= 4; i++) {
            LoginRequest failedRequest = new LoginRequest();
            failedRequest.setEmail(TEST_EMAIL);
            failedRequest.setPassword(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 5th failure from this cycle should still be 401 (not yet locked).
        LoginRequest fifthFailRequest = new LoginRequest();
        fifthFailRequest.setEmail(TEST_EMAIL);
        fifthFailRequest.setPassword(WRONG_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fifthFailRequest)))
                .andExpect(status().isUnauthorized());

        // 6th failure should trigger lockout (429).
        LoginRequest sixthFailRequest = new LoginRequest();
        sixthFailRequest.setEmail(TEST_EMAIL);
        sixthFailRequest.setPassword(WRONG_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sixthFailRequest)))
                .andExpect(status().isTooManyRequests());
    }
}

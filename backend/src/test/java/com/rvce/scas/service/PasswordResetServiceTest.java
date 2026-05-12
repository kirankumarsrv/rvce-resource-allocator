package com.rvce.scas.service;

import com.rvce.scas.dto.request.ForgotPasswordRequest;
import com.rvce.scas.dto.request.ResetPasswordWithTokenRequest;
import com.rvce.scas.dto.response.ForgotPasswordResponseDto;
import com.rvce.scas.entity.Department;
import com.rvce.scas.entity.PasswordResetToken;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.CsvValidationException;
import com.rvce.scas.repository.PasswordResetTokenRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for password reset service.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                emailService
        );
    }

    @Test
    void requestPasswordResetGeneratesTokenAndSendsEmail() {
        UUID userId = UUID.randomUUID();
        String email = "teacher@rvce.edu.in";

        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setName("Dr. Teacher");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(tokenRepository.findAll()).thenReturn(java.util.List.of());
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ForgotPasswordResponseDto response = passwordResetService.requestPasswordReset(
                new ForgotPasswordRequest(email)
        );

        assertNotNull(response);
        assertTrue(response.getMessage().contains("email"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(userId, savedToken.getUser().getUserId());
        assertFalse(savedToken.isUsed());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));

        verify(emailService, times(1)).sendPasswordResetEmail(
                eq(email),
                eq("Dr. Teacher"),
                anyString()
        );
    }

    @Test
    void requestPasswordResetForNonExistentEmailReturnsGenericMessage() {
        String email = "nonexistent@rvce.edu.in";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        ForgotPasswordResponseDto response = passwordResetService.requestPasswordReset(
                new ForgotPasswordRequest(email)
        );

        assertNotNull(response);
        assertTrue(response.getMessage().contains("email"));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resetPasswordWithTokenValidatesAndUpdatesPassword() {
        UUID userId = UUID.randomUUID();
        String newPassword = "NewPassword@123";
        String rawToken = "some-valid-token";

        User user = new User();
        user.setUserId(userId);
        user.setEmail("teacher@rvce.edu.in");
        user.setPasswordHash("old-hash");

        PasswordResetToken token = new PasswordResetToken();
        token.setTokenId(UUID.randomUUID());
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(newPassword)).thenReturn("new-hash-" + newPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> response = passwordResetService.resetPasswordWithToken(
                new ResetPasswordWithTokenRequest(rawToken, newPassword)
        );

        assertTrue(response.containsKey("message"));
        assertTrue(response.get("message").contains("reset successfully"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("new-hash-" + newPassword, userCaptor.getValue().getPasswordHash());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertTrue(tokenCaptor.getValue().isUsed());
        assertNotNull(tokenCaptor.getValue().getUsedAt());
    }

    @Test
    void resetPasswordWithInvalidTokenThrows() {
        String rawToken = "invalid-token";

        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        assertThrows(CsvValidationException.class, () ->
                passwordResetService.resetPasswordWithToken(
                        new ResetPasswordWithTokenRequest(rawToken, "NewPassword@123")
                )
        );
    }
}

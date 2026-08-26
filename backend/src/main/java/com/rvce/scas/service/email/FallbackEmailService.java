package com.rvce.scas.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Fallback email service when no other EmailService bean is available.
 * This lets the application start even when SMTP is not configured.
 */
@Slf4j
@Service
@Profile("prod")
public class FallbackEmailService implements EmailService {

    @Override
    public void sendPasswordResetEmail(String email, String name, String resetLink) {
        log.warn("No EmailService bean configured. Falling back to logging the password reset email.");
        log.info("PASSWORD RESET EMAIL TO: {}", email);
        log.info("RESET LINK: {}", resetLink);
    }
}

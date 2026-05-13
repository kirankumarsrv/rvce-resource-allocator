package com.rvce.scas.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Development email service that logs emails instead of sending them.
 */
@Slf4j
@Service
@Profile("dev")
public class DevEmailService implements EmailService {

    @Override
    public void sendPasswordResetEmail(String email, String name, String resetLink) {
        log.info("DEV: Would send password reset email to {} ({})", email, name);
        log.info("DEV: Reset link: {}", resetLink);
    }
}

package com.rvce.scas.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Development email service that logs emails instead of sending them.
 * Only used when no JavaMailSender bean is available.
 */
@Slf4j
@Service
@Profile("dev")
@ConditionalOnMissingBean(JavaMailSender.class)
public class DevEmailService implements EmailService {

    @Override
    public void sendPasswordResetEmail(String email, String name, String resetLink) {
        log.info("DEV: Would send password reset email to {} ({})", email, name);
        log.info("DEV: Reset link: {}", resetLink);
    }
}

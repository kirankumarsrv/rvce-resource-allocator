package com.rvce.scas.config;

import com.rvce.scas.service.email.EmailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Test-specific Spring configuration for integration tests.
 */
@Configuration
@Profile("test")
@EnableAsync
public class TestConfig {

    @Bean
    public EmailService emailService() {
        return (email, name, resetLink) -> {
            // No-op email service for tests. Avoids requiring SMTP or JavaMailSender in test profile.
        };
    }
}

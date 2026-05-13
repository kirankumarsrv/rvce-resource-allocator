package com.rvce.scas.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Production email service that sends emails via SMTP.
 * Uses Spring's JavaMailSender to dispatch emails.
 * Requires mail configuration properties to be set:
 *   - spring.mail.host
 *   - spring.mail.port
 *   - spring.mail.username
 *   - spring.mail.password
 *   - spring.mail.from (optional, defaults to spring.mail.username)
 */
@Slf4j
@Service
@ConditionalOnBean(JavaMailSender.class)
public class ProductionEmailService implements EmailService {

    private final JavaMailSender mailSender;

    public ProductionEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String email, String name, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setFrom("noreply@rvce.edu.in");
            message.setSubject("Password Reset Request - RVCE SCAS");
            message.setText(buildPasswordResetEmailBody(name, resetLink));

            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Builds the password reset email body.
     */
    private String buildPasswordResetEmailBody(String name, String resetLink) {
        return String.format(
            "Dear %s,\n\n"
                + "You have requested to reset your password for RVCE SCAS.\n\n"
                + "Please click the link below to reset your password:\n"
                + "%s\n\n"
                + "This link will expire in 24 hours.\n\n"
                + "If you did not request this password reset, please ignore this email.\n\n"
                + "Best regards,\n"
                + "RVCE Resource Allocator Team",
            name,
            resetLink
        );
    }
}

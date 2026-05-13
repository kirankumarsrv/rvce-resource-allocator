package com.rvce.scas.service.email;

/**
 * Interface for email dispatch service.
 * Allows pluggable implementations (no-op dev, SMTP, SendGrid, etc).
 */
public interface EmailService {

    /**
     * Send a password reset email with a reset link.
     *
     * @param email recipient email
     * @param name recipient name
     * @param resetLink the reset URL to include in the email
     */
    void sendPasswordResetEmail(String email, String name, String resetLink);
}

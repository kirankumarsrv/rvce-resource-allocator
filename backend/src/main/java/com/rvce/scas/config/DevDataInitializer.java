package com.rvce.scas.config;

import com.rvce.scas.entity.User;
import com.rvce.scas.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * DevDataInitializer
 * 
 * Initializes test user passwords in development environments.
 * Only active when spring.profiles.active=dev
 * 
 * This replaces the hardcoded password hash migration (V10) with
 * a runtime initialization that doesn't expose secrets in git.
 */
@Configuration
@Profile("dev")
public class DevDataInitializer {

    @Bean
    public CommandLineRunner initDevUsers(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        return args -> {
            // Test user emails from the database seed
            String[] devEmails = {
                "admin@rvce.edu.in",
                "tto@rvce.edu.in",
                "priya.sharma@rvce.edu.in",
                "ramesh.kumar@rvce.edu.in",
                "kiran@rvce.edu.in"
            };

            // Standard test password
            String testPassword = "Test@1234";
            String encodedPassword = encoder.encode(testPassword);

            // Reset password for all test accounts if they exist
            for (String email : devEmails) {
                userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                    user.setPasswordHash(encodedPassword);
                    userRepository.save(user);
                    System.out.println("✓ Dev mode: Reset password for " + email);
                });
            }

            System.out.println("✓ Dev data initialization complete. Test password: Test@1234");
        };
    }
}

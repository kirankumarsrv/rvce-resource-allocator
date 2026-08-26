package com.rvce.scas.config;

import com.rvce.scas.security.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Bean
    public EncryptionUtil encryptionUtil(@Value("${scas.encryption.key:}") String base64Key) {
        String activeProfile = System.getProperty("spring.profiles.active",
                System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", ""));
        if (activeProfile.contains("prod") && (base64Key == null || base64Key.isBlank())) {
            throw new IllegalStateException("SCAS_ENCRYPTION_KEY is required when the prod profile is active.");
        }
        EncryptionUtil util = new EncryptionUtil(base64Key);
        // Register the converter immediately after creating the util
        EncryptedStringConverter.setEncryptionUtil(util);
        return util;
    }
}

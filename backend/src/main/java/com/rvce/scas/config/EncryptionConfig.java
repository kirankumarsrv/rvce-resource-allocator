package com.rvce.scas.config;

import com.rvce.scas.security.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EncryptionConfig {

    private EncryptionUtil encryptionUtil;

    @Bean
    public EncryptionUtil encryptionUtil(@Value("${scas.encryption.key:}") String base64Key) {
        this.encryptionUtil = new EncryptionUtil(base64Key);
        return this.encryptionUtil;
    }

    @PostConstruct
    public void registerConverter() {
        EncryptedStringConverter.setEncryptionUtil(encryptionUtil);
    }
}

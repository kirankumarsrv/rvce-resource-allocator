package com.rvce.scas.config;

import com.rvce.scas.security.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Bean
    public EncryptionUtil encryptionUtil(@Value("${scas.encryption.key:}") String base64Key) {
        EncryptionUtil util = new EncryptionUtil(base64Key);
        // Register the converter immediately after creating the util
        EncryptedStringConverter.setEncryptionUtil(util);
        return util;
    }
}

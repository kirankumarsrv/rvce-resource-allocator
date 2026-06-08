package com.rvce.scas.config;

import com.rvce.scas.security.EncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that transparently encrypts and decrypts String fields.
 */
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionUtil encryptionUtil;

    public static void setEncryptionUtil(EncryptionUtil util) {
        encryptionUtil = util;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        if (encryptionUtil == null) {
            throw new IllegalStateException("EncryptionUtil is not initialized for JPA AttributeConverter");
        }
        return encryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        if (encryptionUtil == null) {
            throw new IllegalStateException("EncryptionUtil is not initialized for JPA AttributeConverter");
        }
        try {
            return encryptionUtil.decrypt(dbData);
        } catch (Exception e) {
            // If decryption fails, assume the data is plain text (legacy data)
            // This handles migration from unencrypted to encrypted fields
            return dbData;
        }
    }
}

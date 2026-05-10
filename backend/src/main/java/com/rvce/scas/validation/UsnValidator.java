package com.rvce.scas.validation;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validator for RVCE USN values.
 */
@Component
public class UsnValidator {

    public static final String USN_REGEX = "^1RV\\d{2}[A-Z]{2}\\d{3}$";

    private static final Pattern PATTERN = Pattern.compile(USN_REGEX);

    public boolean isValid(String usn) {
        return usn != null && PATTERN.matcher(usn).matches();
    }

    public String normalize(String usn) {
        return usn == null ? null : usn.trim().toUpperCase(Locale.ROOT);
    }
}

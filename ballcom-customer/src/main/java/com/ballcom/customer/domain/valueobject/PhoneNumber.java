package com.ballcom.customer.domain.valueobject;

import java.util.regex.Pattern;

public record PhoneNumber(String value) {
    
    private static final Pattern REGEX = Pattern.compile("^(\\+|00)[1-9][0-9\\s\\-]{6,14}$");

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Telefoonnummer mag niet leeg zijn");
        }
        
        if (!REGEX.matcher(value).matches()) {
            throw new IllegalArgumentException("Ongeldig telefoonnummer formaat " + value);
        }
    }
}
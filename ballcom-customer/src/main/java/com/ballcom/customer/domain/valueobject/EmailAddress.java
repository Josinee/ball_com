package com.ballcom.customer.domain.valueobject;

public record EmailAddress(String value) {
    public EmailAddress {
        if(value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

}

package com.ballcom.customer.domain.valueobject;

public record Address(String street, String houseNumber, String city, String zipCode) {
    public Address{
        if (street == null || street.isBlank()) throw new IllegalArgumentException("Street is invalid");
        if (houseNumber == null || houseNumber.isBlank()) throw new IllegalArgumentException("Housenumber is invalid");
        if (city == null || city.isBlank()) throw new IllegalArgumentException("City is invalid");
        if (zipCode == null || zipCode.isBlank()) throw new IllegalArgumentException("Zipcode is invalid");
//TODO verdere validatie??
    }
}
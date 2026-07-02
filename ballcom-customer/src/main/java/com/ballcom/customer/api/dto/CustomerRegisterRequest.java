package com.ballcom.customer.api.dto;

public record CustomerRegisterRequest(String name,
    String email,
    String street,
    String houseNumber,
    String city,
    String zipCode
) {}

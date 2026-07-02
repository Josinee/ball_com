package com.ballcom.customer.api.dto;

import java.util.UUID;

public record CustomerViewResponse(
    UUID customerId,
    String companyName,
    String firstName,
    String lastName,
    String phoneNumber,
    String street,
    String houseNumber,
    String city,
    String zipCode
) {}

package com.ballcom.customer.api.dto;

import java.util.UUID;

public record CustomerViewResponse(
    UUID customerId,
    String name,
    String email,
    String street,
    String houseNumber,
    String city,
    String zipCode
) {}

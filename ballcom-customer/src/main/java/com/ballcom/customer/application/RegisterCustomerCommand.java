package com.ballcom.customer.application;


public record RegisterCustomerCommand(
    String companyName,
    String firstName,
    String lastName,
    String phoneNumber,
    String street,
    String houseNumber,
    String city,
    String zipCode
) {}
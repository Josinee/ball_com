package com.ballcom.customer.application;


public record RegisterCustomerCommand(
    String name,
    String email,
    String street,       
    String houseNumber,
    String city,
    String zipCode
) {}
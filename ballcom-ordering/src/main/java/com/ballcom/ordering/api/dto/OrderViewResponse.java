package com.ballcom.ordering.api.dto;

import java.math.BigDecimal;

import java.util.UUID;


//geeft het order bij een GET request
public record OrderViewResponse(
    UUID orderId,
    UUID customerId,
    BigDecimal totalAmount,
    String order_status,
    String payment_status
) {}
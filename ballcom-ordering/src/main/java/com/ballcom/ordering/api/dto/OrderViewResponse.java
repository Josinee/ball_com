package com.ballcom.ordering.api.dto;

import java.math.BigDecimal;

import java.util.UUID;



public record OrderViewResponse(
    UUID orderId,
    UUID customerId,
    BigDecimal totalAmount,
    String status
) {}
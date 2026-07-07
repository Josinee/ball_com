package com.ballcom.payment.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.ballcom.payment.domain.PaymentMethod;

public record ProcessPaymentCommand(UUID paymentId, UUID customerId, UUID orderId, BigDecimal total, PaymentMethod paymentMethod) {}
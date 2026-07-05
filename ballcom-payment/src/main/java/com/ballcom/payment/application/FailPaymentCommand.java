package com.ballcom.payment.application;

import java.util.UUID;

public record FailPaymentCommand (UUID orderId, String reason) {}
    


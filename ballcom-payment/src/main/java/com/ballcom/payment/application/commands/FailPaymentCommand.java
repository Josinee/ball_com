package com.ballcom.payment.application.commands;

import java.util.UUID;

public record FailPaymentCommand (UUID orderId, String reason) {}
    


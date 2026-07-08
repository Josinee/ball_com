package com.ballcom.payment.application.commands;

import java.util.UUID;

public record CompletePaymentCommand (UUID orderId) {}
    


package com.ballcom.payment.application;

import java.util.UUID;

public record CompletePaymentCommand (UUID orderId) {}
    


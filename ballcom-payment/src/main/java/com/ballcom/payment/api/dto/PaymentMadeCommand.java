package com.ballcom.payment.api.dto;

import java.util.UUID;

public record PaymentMadeCommand(UUID paymentId) {
    
}

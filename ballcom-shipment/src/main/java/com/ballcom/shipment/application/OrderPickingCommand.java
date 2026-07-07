package com.ballcom.shipment.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPickingCommand(UUID customerId, UUID orderId, BigDecimal totalPrice) {
    
}

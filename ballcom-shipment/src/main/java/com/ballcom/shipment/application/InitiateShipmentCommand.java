package com.ballcom.shipment.application;

import java.util.List;
import java.util.UUID;

public record InitiateShipmentCommand(UUID orderId, List<?> items) {
    
}

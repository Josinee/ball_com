package com.ballcom.shipment.application.dto;

import java.util.List;
import java.util.UUID;

public record InitiateShipmentCommand(UUID shipmentId, UUID orderId, List<?> items) {
    
}

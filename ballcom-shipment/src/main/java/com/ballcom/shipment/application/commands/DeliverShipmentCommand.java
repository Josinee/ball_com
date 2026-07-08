package com.ballcom.shipment.application.commands;

import java.util.UUID;


public record DeliverShipmentCommand(UUID orderId) {
    
}

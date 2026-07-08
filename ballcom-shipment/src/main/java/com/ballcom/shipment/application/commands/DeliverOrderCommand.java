package com.ballcom.shipment.application.commands;

import java.util.UUID;


public record DeliverOrderCommand(UUID orderId) {
    
}

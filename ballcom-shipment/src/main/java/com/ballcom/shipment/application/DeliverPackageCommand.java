package com.ballcom.shipment.application;

import java.util.UUID;


public record DeliverPackageCommand(UUID orderId) {
    
}

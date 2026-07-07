package com.ballcom.shipment.api;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ballcom.shipment.application.ShipmentCommandHandler;
import com.ballcom.shipment.application.ShipPackageCommand;
import com.ballcom.shipment.application.DeliverPackageCommand;

@RestController
@RequestMapping("/shipments")
public class ShipmentCommandController {
    private final ShipmentCommandHandler commandHandler;

    public ShipmentCommandController(ShipmentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    //Handmatig op shipped zetten
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<String> shipPackage(@PathVariable UUID orderId) {
        commandHandler.handlePackageShipped(new ShipPackageCommand(orderId));
        return ResponseEntity.ok("Package marked as SHIPPED for order " + orderId);
    }

    //Handmatig op delivered zetten
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<String> deliverPackage(@PathVariable UUID orderId) {
        commandHandler.handleDeliveryCompleted(new DeliverPackageCommand(orderId));
        return ResponseEntity.ok("Package marked as DELIVERED for order " + orderId);
    }
}
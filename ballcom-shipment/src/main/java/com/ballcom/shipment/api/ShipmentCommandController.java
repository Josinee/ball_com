package com.ballcom.shipment.api;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ballcom.shipment.application.ShipmentCommandHandler;
import com.ballcom.shipment.application.dto.DeliverOrderCommand;
import com.ballcom.shipment.application.dto.ShipOrderCommand;

@RestController
@RequestMapping("/shipments")
public class ShipmentCommandController {
    private final ShipmentCommandHandler commandHandler;

    public ShipmentCommandController(ShipmentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    //Handmatig op shipped zetten
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<String> shipOrder(@PathVariable UUID orderId) {
        commandHandler.handleOrderShipped(new ShipOrderCommand(orderId));
        return ResponseEntity.ok("Order marked as SHIPPED for order " + orderId);
    }

    //Handmatig op delivered zetten
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<String> deliverOrder(@PathVariable UUID orderId) {
        commandHandler.handleDeliveryCompleted(new DeliverOrderCommand(orderId));
        return ResponseEntity.ok("Order marked as DELIVERED for order " + orderId);
    }
}
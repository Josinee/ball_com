package com.ballcom.shipment.api;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ballcom.shipment.application.ShipmentCommandHandler;
import com.ballcom.shipment.application.commands.DeliverOrderCommand;
import com.ballcom.shipment.application.commands.ShipOrderCommand;

@RestController
@RequestMapping("/shipments")
public class ShipmentCommandController {
    private final ShipmentCommandHandler commandHandler;

    public ShipmentCommandController(ShipmentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    //Handmatig op shipped zetten
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable UUID orderId) {
        UUID shipmentId = commandHandler.handleOrderShipped(new ShipOrderCommand(orderId));
        return ResponseEntity.accepted().body(new ShipOrderCommand(shipmentId)); 
    }

    //Handmatig op delivered zetten
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable UUID orderId) {
        UUID shipmentId = commandHandler.handleDeliveryCompleted(new DeliverOrderCommand(orderId));
        return ResponseEntity.accepted().body(new ShipOrderCommand(shipmentId)); 
    }
}
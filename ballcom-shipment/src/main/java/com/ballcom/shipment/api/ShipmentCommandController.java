package com.ballcom.shipment.api;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ballcom.shipment.application.ShipmentCommandHandler;
import com.ballcom.shipment.application.commands.DeliverShipmentCommand;
import com.ballcom.shipment.application.commands.ShipShipmentCommand;
import com.ballcom.shipment.application.commands.ShipmentResponse;

@RestController
@RequestMapping("/shipments")
public class ShipmentCommandController {
    private final ShipmentCommandHandler commandHandler;

    public ShipmentCommandController(ShipmentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable UUID orderId) {
        UUID shipmentId = commandHandler.handleOrderShipped(new ShipShipmentCommand(orderId));
        return ResponseEntity.accepted().body(new ShipmentResponse(shipmentId, "SHIPPED")); 
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable UUID orderId) {
        UUID shipmentId = commandHandler.handleDeliveryCompleted(new DeliverShipmentCommand(orderId));
        return ResponseEntity.accepted().body(new ShipmentResponse(shipmentId, "DELIVERED")); 
    }
}
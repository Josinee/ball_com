package com.ballcom.shipment.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;

public class ShipmentAggregate extends AggregateRoot {

    private UUID orderId;
    private ShipmentStatus status;
    private String carrier;
    private double shippingPrice;

    public ShipmentAggregate() {}

    public static ShipmentAggregate initiate(UUID shipmentId, UUID orderId, String carrier, double shippingPrice) {
        ShipmentAggregate shipment = new ShipmentAggregate();
        shipment.id = shipmentId;

        Map<String, Object> payload = Map.of(
            "orderId", orderId.toString(),
            "status", "AWAITING_PAYMENT", // Wacht op groen licht van Payment
            "carrier", carrier,
            "shippingPrice", String.valueOf(shippingPrice)
        );

        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(), shipmentId, shipment.getSequenceNumber() + 1,
            EventType.COSTS_CALCULATED, Instant.now(), payload
        );
        shipment.raiseEvent(event);
        return shipment;
    }

    public void releaseToWarehouse() {
        if(this.status== ShipmentStatus.PICKING || this.status == ShipmentStatus.SHIPPED || this.status == ShipmentStatus.DELIVERED) {
            return;
        }

        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(), this.getId(), this.getSequenceNumber() + 1,
            EventType.ORDER_PICKED, Instant.now(),
            Map.of("orderId", this.orderId.toString(), "status", "PICKING")
        );
        this.raiseEvent(event);
    }

    public void markAsShipped(String carrier, double shippingPrice) {
        if (this.status != ShipmentStatus.PICKING) {
            throw new IllegalStateException("Not allowed to move to state SHIPPED when in state " + this.status);
        }
        this.emitShipped(carrier, shippingPrice);
    }

    public void markAsDelivered() {
        if (this.status != ShipmentStatus.SHIPPED) {
            throw new IllegalStateException("Not allowed to move to state DELIVERED when in state " + this.status);
        }
        this.emitDelivered();
    }


    public void emitShipped(String carrier, double shippingPrice) {
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),
            this.getId(),
            this.getSequenceNumber() + 1,
            EventType.ORDER_SHIPPED,
            Instant.now(),
            Map.of(
                "status", "SHIPPED",
                "orderId", this.orderId.toString(),
                "carrier", carrier,
                "shippingPrice", String.valueOf(shippingPrice)
            )
        );
        this.raiseEvent(event);
    }

    public void emitDelivered() {
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),
            this.getId(),
            this.getSequenceNumber() + 1,
            EventType.ORDER_DELIVERED,
            Instant.now(),
            Map.of(
                "status", "DELIVERED",
                "orderId", this.orderId.toString()
            )
        );
        this.raiseEvent(event);
    }




    @Override
    public void apply(GenericDomainEvent event) {
        this.id = event.aggregateId();
        Map<String, Object> payload = event.payload();
        
        if (EventType.COSTS_CALCULATED.equals(event.eventType())) {
            this.orderId = UUID.fromString((String) payload.get("orderId"));
            this.carrier = (String) payload.get("carrier");
            this.shippingPrice = Double.parseDouble((String) payload.get("shippingPrice"));
            this.status = ShipmentStatus.AWAITING_PAYMENT;
        } 
        else if (EventType.ORDER_PICKED.equals(event.eventType())) {
            this.status = ShipmentStatus.PICKING;
        } 
        else if (EventType.ORDER_SHIPPED.equals(event.eventType())) {
            if (payload.containsKey("carrier")) {
                this.carrier = (String) payload.get("carrier");
            }
            if (payload.containsKey("shippingPrice")) {
                this.shippingPrice = Double.parseDouble(String.valueOf(payload.get("shippingPrice")));
            }
            this.status = ShipmentStatus.SHIPPED;
        } 
        else if (EventType.ORDER_DELIVERED.equals(event.eventType())) {
            this.status = ShipmentStatus.DELIVERED;
        }
    }

    public void setId(UUID newId) {
        this.id = newId;
    }

    public String getCarrier() {
        return carrier;
    }

    public double getShippingPrice() {
        return shippingPrice;
    }
    
}
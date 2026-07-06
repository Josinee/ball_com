package com.ballcom.shipment.domain;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;

public class ShipmentAggregate extends AggregateRoot {

    private UUID orderId;
    private ShipmentStatus status;
    private String carrier;
    private double shippingPrice;

    public ShippingAggregate() {}

    public static ShippingAggregate initiate(UUID orderId, ShipmentStatus status) {
        ShipmentAggregate shipment = new ShipmentAggregate();
        UUID shipmentId = UUID.randomUUID();

        Map<String, Object> payload = Map.of(
            "orderId", orderId.toString(),
            "status", ShipmentStatus.PICKING
        )

        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),
            shipmentId,
            shipment.getSequenceNumber() + 1,
            EventType.ORDER_PICKED,
            Instant.now(),
            payload
        );
    }

    @Override
    protected void apply(GenericDomainEvent event) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'apply'");
    }
    
}
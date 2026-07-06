package com.ballcom.shipment.application;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import com.ballcom.shipment.domain.ShipmentAggregate;
import com.ballcom.shipment.domain.ShipmentStatus;

@Component
public class ShipmentCommandHandler{
    private final EventStore eventStore;

    public ShipmentCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public void handle(OrderPickingCommand command) {
        ShipmentAggregate shipment = ShipmentAggregate.initiate(command.orderId());
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();
    }

    @Transactional
    public void handle(ShipPackageCommand command) {
        List<GenericDomainEvent> history = eventStore.loadEventsByOrderId(command.orderId());
    }
}
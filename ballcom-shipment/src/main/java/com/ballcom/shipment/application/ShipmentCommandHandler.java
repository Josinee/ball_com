package com.ballcom.shipment.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import com.ballcom.shipment.domain.ShipmentAggregate;

@Component
public class ShipmentCommandHandler{
    private final EventStore eventStore;

    public ShipmentCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public void handleOrderPlaced(InitiateShipmentCommand command) {

        String carrier;
        double price;
        // grote bestellingen met postnl, kleinere bestellingen met dhl
        //mock van logistieke bedrijven en de kosten hiervan
        if (command.items().size() > 4) {
            carrier = "PostNL";
            price = 6.95;
        } else {
            carrier = "DHL";
            price = 5.50;
        }

        ShipmentAggregate shipment = ShipmentAggregate.initiate(command.orderId(), carrier, price);
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();
    }

    @Transactional
    public void handlePaymentApproved(OrderPickingCommand command) {
        List<GenericDomainEvent> history = eventStore.loadEventsById(command.orderId());
        ShipmentAggregate shipment = new ShipmentAggregate();
        shipment.loadFromHistory(history);

        shipment.releaseToWarehouse();
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();
    }

    @Transactional
    public void handleDeliveryCompleted(DeliverPackageCommand command) {
        List<GenericDomainEvent> history = eventStore.loadEventsById(command.orderId());
        ShipmentAggregate shipment = new ShipmentAggregate();
        shipment.loadFromHistory(history);

        shipment.markAsDelivered();
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();
    }
}
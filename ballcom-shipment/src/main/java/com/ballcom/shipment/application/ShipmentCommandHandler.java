package com.ballcom.shipment.application;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import com.ballcom.shipment.application.commands.DeliverShipmentCommand;
import com.ballcom.shipment.application.commands.InitiateShipmentCommand;
import com.ballcom.shipment.application.commands.ReleaseShipmentToWarehouse;
import com.ballcom.shipment.application.commands.ShipShipmentCommand;
import com.ballcom.shipment.domain.ShipmentAggregate;

@Component
public class ShipmentCommandHandler  {
    private final EventStore eventStore;
    private JdbcTemplate jdbcTemplate;

    public ShipmentCommandHandler(EventStore eventStore, JdbcTemplate jdbcTemplate) {
        this.eventStore = eventStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void handleOrderPlaced(InitiateShipmentCommand command) {

        String carrier;
        double price;
        // grote bestellingen met postnl, kleinere bestellingen met dhl
        //  mock van logistieke bedrijven en de kosten hiervan
        if (command.items().size() > 4) {
            carrier = "PostNL";
            price = 6.95;
        } else {
            carrier = "DHL";
            price = 5.50;
        }

        ShipmentAggregate shipment = ShipmentAggregate.initiate(command.shipmentId(), command.orderId(), carrier, price);
        
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());

        shipment.clearUncommitedEvents();
    }

    @Transactional
    public void handleReleaseToWarehouse(ReleaseShipmentToWarehouse command) {

        List<GenericDomainEvent> history = eventStore.loadEvents(command.shipmentId());
        if (history == null || history.isEmpty()) {
            throw new RuntimeException("Fout bij PaymentApproved: De mapping bestaat (shipmentId: " + command.shipmentId() + "), maar er zijn GEEN events gevonden in de event_store voor deze shipment. Is het aanmaken van de shipment gecrasht?");
        }
        ShipmentAggregate shipment = new ShipmentAggregate();
        shipment.loadFromHistory(history);

        shipment.releaseToWarehouse();
        
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();

    }

    public UUID handleOrderShipped(ShipShipmentCommand command) {

        String sql = "SELECT shipment_id FROM order_shipment_mapping WHERE order_id = ?";
        UUID shipmentId = jdbcTemplate.queryForObject(sql, UUID.class, command.orderId());

        List<GenericDomainEvent> history = eventStore.loadEvents(shipmentId);
        ShipmentAggregate shipment = new ShipmentAggregate();
        shipment.loadFromHistory(history);
        shipment.markAsShipped(shipment.getCarrier(), shipment.getShippingPrice());
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();
        return shipment.getId();
    }

    @Transactional
    public UUID handleDeliveryCompleted(DeliverShipmentCommand command) {

        String sql = "SELECT shipment_id FROM order_shipment_mapping WHERE order_id = ?";
        UUID shipmentId = jdbcTemplate.queryForObject(sql, UUID.class, command.orderId());


        List<GenericDomainEvent> history = eventStore.loadEvents(shipmentId);
        ShipmentAggregate shipment = new ShipmentAggregate();
        shipment.loadFromHistory(history);

        shipment.markAsDelivered();
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();
        return shipment.getId();
    }

    


}
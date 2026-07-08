package com.ballcom.shipment.application;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import com.ballcom.shipment.application.dto.DeliverOrderCommand;
import com.ballcom.shipment.application.dto.InitiateShipmentCommand;
import com.ballcom.shipment.application.dto.OrderPickingCommand;
import com.ballcom.shipment.application.dto.ShipOrderCommand;
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
    public void handleReleaseToWarehouse(OrderPickingCommand command) {
        //haal shipmentid uit orderid mapping
        String sql = "SELECT shipment_id FROM order_shipment_mapping WHERE order_id = ?";
        UUID shipmentId;
        
        try {
            shipmentId = jdbcTemplate.queryForObject(sql, UUID.class, command.orderId());
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // Als dit gegooid wordt, is de INSERT in de listener om een of andere reden nooit uitgevoerd of mislukt!
            throw new RuntimeException("Fout bij PaymentApproved: Geen shipment_id mapping gevonden in 'order_shipment_mapping' voor orderId: " + command.orderId());
        }
        // laad event history
        List<GenericDomainEvent> history = eventStore.loadEvents(shipmentId);
        if (history == null || history.isEmpty()) {
            throw new RuntimeException("Fout bij PaymentApproved: De mapping bestaat (shipmentId: " + shipmentId + "), maar er zijn GEEN events gevonden in de event_store voor deze shipment. Is het aanmaken van de shipment gecrasht?");
        }
        ShipmentAggregate shipment = new ShipmentAggregate();
        shipment.loadFromHistory(history);

        shipment.releaseToWarehouse();
        
        eventStore.append(shipment.getId(), shipment.getUncommitedEvents(), shipment.getExpectedVersion());
        shipment.clearUncommitedEvents();

    }

    public UUID handleOrderShipped(ShipOrderCommand command) {
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
    public UUID handleDeliveryCompleted(DeliverOrderCommand command) {

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
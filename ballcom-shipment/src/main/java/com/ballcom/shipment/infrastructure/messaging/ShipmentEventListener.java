package com.ballcom.shipment.infrastructure.messaging;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shipment.application.DeliverPackageCommand;
import com.ballcom.shipment.application.InitiateShipmentCommand;
import com.ballcom.shipment.application.OrderPickingCommand;
import com.ballcom.shipment.application.ShipmentCommandHandler;
import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent; 

@Component
public class ShipmentEventListener {
    private final JdbcTemplate jdbcTemplate;
    private final ShipmentCommandHandler commandHandler;

    public ShipmentEventListener(JdbcTemplate jdbcTemplate, ShipmentCommandHandler commandHandler) {
        this.jdbcTemplate = jdbcTemplate;
        this.commandHandler = commandHandler;
    }

    //Moet naar ORDER_PLACED luisteren zodat de goedkoopste carrier bepaald wordt

    //Luistert naar PAYMENT_COMPLETED of PAYMENT_AWAITING_DELIVERY om het pakket daadwerkelijk naar PICKING te kunnen zetten



    @RabbitListener(queues = "shipping-order-placed-queue") 
    public void consumeOrderPlaced(GenericDomainEvent event) {

        if(EventType.ORDER_PLACED.equals(event.eventType())){
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if (rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + " al eerder verwerkt");
                return;
            }

            Map<String, Object> payload = (Map<String, Object>) event.payload();
            
            UUID orderId = event.aggregateId();
            List<?> items = (List<?>) payload.get("items");
            // shipment berekent goedkoopste carrier mock
            var command = new InitiateShipmentCommand(orderId, items);
            commandHandler.handleOrderPlaced(command);
        }

        
    }

    @RabbitListener(queues = "shipping-delivery-queue")
    @Transactional
    public void consumeDelivery(GenericDomainEvent event) {
        if (!EventType.PACKAGE_DELIVERED.equals(event.eventType())) {
            return;
        }

        Map<String, Object> payload = (Map<String, Object>) event.payload();
        UUID orderId = UUID.fromString((String) payload.get("orderId"));

        System.out.println("Shipment delivery ontvangen voor order " + orderId);
        commandHandler.handleDeliveryCompleted(new DeliverPackageCommand(orderId));
    }
    

    @RabbitListener(queues = "shipping-payment-made-queue")
    @Transactional
    public void consume(GenericDomainEvent event) {
        System.out.println("=== CONSUMER ONTVANGEN ===");
            System.out.println("RABBITMQ: Bericht ontvangen uit shipping-payment-made-queue. Event ID: " + event.eventId() + ", Type: " + event.eventType());

            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if (rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + " al eerder verwerkt");
                return;
            }
            Map<String, Object> payload = (Map<String, Object>) event.payload();
         if (EventType.PAYMENT_COMPLETED.equals(event.eventType()) || EventType.PAYMENT_AWAITING_DELIVERY.equals(event.eventType())) {


            var command = new OrderPickingCommand(
                UUID.fromString((String) payload.get("customerId")),
                event.aggregateId(),
                new BigDecimal(payload.get("totalAmount").toString())
            );

            commandHandler.handlePaymentApproved(command);
        } else if(EventType.PAYMENT_AWAITING_DELIVERY.equals(event.eventType())) {
            System.out.println("Afterpay, betaling is in wacht tot delivery.");
        }
    }


}

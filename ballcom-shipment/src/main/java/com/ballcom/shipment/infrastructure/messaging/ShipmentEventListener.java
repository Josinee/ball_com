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
import com.ballcom.shipment.application.ShipPackageCommand;
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
    @Transactional
    public void consumeOrderPlaced(GenericDomainEvent event) {
        System.out.println("=== ORDER PLACED CONSUMER HOOK ===");
        System.out.println("RABBITMQ: Bericht ontvangen in shipping-order-placed-queue.");
        System.out.println("Binnenkomend Event Type: " + event.eventType());
        System.out.println("==================================");
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

            UUID shipmentId = UUID.randomUUID();
            String mappingSql = """
                INSERT INTO order_shipment_mapping (order_id, shipment_id) 
                VALUES (?, ?) ON CONFLICT (order_id) DO NOTHING
            """;
            jdbcTemplate.update(mappingSql, orderId, shipmentId);

            shipmentId = jdbcTemplate.queryForObject(
                "SELECT shipment_id FROM order_shipment_mapping WHERE order_id = ?", 
                UUID.class, 
                orderId
            );


           String viewSql = """
                INSERT INTO shipment_views (shipment_id, order_id, carrier, shipping_price, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING;
            """;
            jdbcTemplate.update(viewSql, shipmentId, orderId, payload.get("carrier"), payload.get("shippingPrice"), "PENDING_PAYMENT", Timestamp.from(event.occurredAt()));
            System.out.println("SHIPMENT VIEW: Record aangemaakt (PENDING_PAYMENT) voor order: " + orderId);

            var command = new InitiateShipmentCommand(shipmentId, orderId, items);
            commandHandler.handleOrderPlaced(command);
        }

        
    }


    @RabbitListener(queues = "shipping-payment-made-queue")
    @Transactional
    public void consumePaymentCompleted(GenericDomainEvent event) {
        System.out.println("=== CONSUMER ONTVANGEN ===");
            System.out.println("RABBITMQ: Bericht ontvangen uit shipping-payment-made-queue. Event ID: " + event.eventId() + ", Type: " + event.eventType());

            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if (rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + " al eerder verwerkt");
                return;
            }
            Map<String, Object> payload = (Map<String, Object>) event.payload();
            UUID orderId = UUID.fromString((String) payload.get("orderId"));
        if (EventType.PAYMENT_COMPLETED.equals(event.eventType())) {
            System.out.println("SHIPMENT: Betaling ontvangen voor order " + orderId + ". Start picking proces.");
            
            var command = new OrderPickingCommand(orderId);
            commandHandler.handlePaymentApproved(command);

            String viewSql = """
                UPDATE shipment_views 
                SET status = 'PICKING', updated_at = ? 
                WHERE order_id = ?
            """;
            jdbcTemplate.update(viewSql, Timestamp.from(event.occurredAt()), orderId);
            System.out.println("SHIPMENT VIEW: Status bijgewerkt naar PICKING voor order: " + orderId);

        }

        else if (EventType.PAYMENT_AWAITING_DELIVERY.equals(event.eventType())) {
            System.out.println("SHIPMENT: AfterPay gedetecteerd voor order " + orderId + ". Shipment aggregate staat in status WAITING_FOR_DELIVERY. Wordt nu nog niet gepickt.");
           
            String viewSql = """
                UPDATE shipment_views 
                SET status = 'WAITING_FOR_DELIVERY', updated_at = ? 
                WHERE order_id = ?
            """;
            jdbcTemplate.update(viewSql, Timestamp.from(event.occurredAt()), orderId);
            System.out.println("SHIPMENT VIEW: Status bijgewerkt naar WAITING_FOR_DELIVERY voor order: " + orderId);
        }
    }

    @RabbitListener(queues = "shipping-delivery-queue")
    @Transactional
    public void consumeDelivery(GenericDomainEvent event) {
        String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
        int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
        if (rowsAffected == 0) return;

        Map<String, Object> payload = (Map<String, Object>) event.payload();
        UUID orderId = UUID.fromString((String) payload.get("orderId"));

        if (EventType.PACKAGE_SHIPPED.equals(event.eventType())) {
            System.out.println("Shipment: Pakket is overgedragen aan carrier voor order " + orderId);

            String viewSql = """
                UPDATE shipment_views 
                SET status = 'SHIPPED', updated_at = ? 
                WHERE order_id = ?
            """;
            jdbcTemplate.update(viewSql, Timestamp.from(event.occurredAt()), orderId);

            System.out.println("SHIPMENT VIEW: Status bijgewerkt naar SHIPPED voor order: " + orderId);
        } 
        else if (EventType.PACKAGE_DELIVERED.equals(event.eventType())) {
            System.out.println("Shipment: Pakket succesvol bezorgd voor order " + orderId);

            String viewSql = """
                UPDATE shipment_views 
                SET status = 'DELIVERED', updated_at = ? 
                WHERE order_id = ?
            """;
            jdbcTemplate.update(viewSql, Timestamp.from(event.occurredAt()), orderId);

            System.out.println("SHIPMENT VIEW: Status bijgewerkt naar DELIVERED voor order: " + orderId);
        }
    }
}

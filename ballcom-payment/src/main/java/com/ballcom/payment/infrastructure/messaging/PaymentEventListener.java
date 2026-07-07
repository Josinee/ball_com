package com.ballcom.payment.infrastructure.messaging;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.payment.application.CompletePaymentCommand;
import com.ballcom.payment.application.PaymentCommandHandler;
import com.ballcom.payment.application.ProcessPaymentCommand;
import com.ballcom.payment.domain.PaymentMethod;
import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent; 

@Component
public class PaymentEventListener {
    private final JdbcTemplate jdbcTemplate;
    private final PaymentCommandHandler commandHandler;

    public PaymentEventListener(JdbcTemplate jdbcTemplate, PaymentCommandHandler commandHandler) {
        this.jdbcTemplate = jdbcTemplate;
        this.commandHandler = commandHandler;
    }

    @RabbitListener(queues = "payment-order-placed-queue")
    @Transactional
    public void consume(GenericDomainEvent event) {
        System.out.println("=== CONSUMER ONTVANGEN ===");
        System.out.println("RABBITMQ: Bericht ontvangen uit payment-order-placed-queue. Event ID: " + event.eventId() + ", Type: " + event.eventType());

        String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
        int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
        

        if (rowsAffected == 0) {
            System.out.println("Event " + event.eventId() + "al eerder verwerkt");
            return;
        }

        Map<String, Object> payload = (Map<String, Object>) event.payload();
        // als er een order geplaatst wordt, worden de gegevens in de database payment_views opgeslagen
        if (EventType.ORDER_PLACED.equals(event.eventType())) {
            String methodFromEvent = (String) payload.get("paymentMethod");
            PaymentMethod mappedMethod = PaymentMethod.valueOf(methodFromEvent.toUpperCase());
            BigDecimal productAmount = new BigDecimal(payload.get("totalAmount").toString());

            String sql = """
                INSERT INTO payment_views (payment_id, order_id, customer_id, total_amount, payment_method, status, updated_at)
                VALUES (?, ?, ?, ?, ?, 'INITIATED', ?)
                ON CONFLICT (order_id) DO NOTHING
            """;
            jdbcTemplate.update(sql, UUID.randomUUID(), event.aggregateId(), UUID.fromString((String) payload.get("customerId")), productAmount, mappedMethod, Timestamp.from(event.occurredAt()));
            

        } else if (EventType.SHIPPING_COSTS_CALCULATED.equals(event.eventType())) {
            UUID orderId = UUID.fromString((String) payload.get("orderId"));
            BigDecimal shippingPrice = new BigDecimal(payload.get("shippingPrice").toString());
            // krijg prijs van totale producten, voeg shipping cost erbij en update de status naar INITIATED
            String updateSql = "UPDATE payment_views SET total_amount = total_amount + ?, status = 'INITIATED' WHERE order_id = ? RETURNING total_amount, customer_id, payment_method";
            Map<String, Object> updatedRow = jdbcTemplate.queryForMap(updateSql, shippingPrice, orderId);
            BigDecimal finalTotal = (BigDecimal) updatedRow.get("total_amount");
            
            var command = new ProcessPaymentCommand(
                UUID.fromString((String) updatedRow.get("customer_id")),
                orderId,
                finalTotal,
                PaymentMethod.valueOf(((String) updatedRow.get("payment_method")).toUpperCase())
            );

            commandHandler.handle(command);
            System.out.println("PAYMENT: Verzendkosten (€" + shippingPrice + ") toegevoegd! Totaalbedrag is nu: €" + finalTotal);
        }
        

    }

    @RabbitListener(queues = "payment-shipment-delivery-queue")
    @Transactional
    public void consumeDelivery(GenericDomainEvent event) {
        if (!EventType.PACKAGE_DELIVERED.equals(event.eventType())) {
            return;
        }

        Map<String, Object> payload = (Map<String, Object>) event.payload();
        UUID orderId = UUID.fromString((String) payload.get("orderId"));

        System.out.println("Delivery ontvangen voor order " + orderId + ". Betaling wordt afgerond.");
        commandHandler.handle(new CompletePaymentCommand(orderId));
    }

    @RabbitListener(queues = "payment-readmodel-queue")
    @Transactional
    public void consumePaymentLifecycleEvents(GenericDomainEvent event) {
        System.out.println("=== CONSUMER ONTVANGEN ===");
        System.out.println("RABBITMQ: Bericht ontvangen uit payment-readmodel-queue. Event ID: " + event.eventId() + ", Type: " + event.eventType());
        // Idempotency check voor payment events
        String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
        int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
        if (rowsAffected == 0) return;

        Map<String, Object> payload = (Map<String, Object>) event.payload();
        UUID paymentId = event.aggregateId(); 

        if (EventType.PAYMENT_INITIATED.equals(event.eventType())) {
            UUID orderId = UUID.fromString((String) payload.get("orderId"));

            String sql = "UPDATE payment_views SET payment_id = ?, status = 'INITIATED', updated_at = ? WHERE order_id = ?";
            jdbcTemplate.update(sql, paymentId, Timestamp.from(event.occurredAt()), orderId);
            System.out.println("READ MODEL: Payment " + paymentId + " staat nu definitief op INITIATED");
        }
        
        else if (EventType.PAYMENT_COMPLETED.equals(event.eventType())) {
            String sql = "UPDATE payment_views SET status = 'COMPLETED', updated_at = ? WHERE payment_id = ?";
            jdbcTemplate.update(sql, Timestamp.from(event.occurredAt()), paymentId);
            System.out.println("READ MODEL: Payment " + paymentId + " staat nu op COMPLETED");
        } 
        
        else if (EventType.PAYMENT_FAILED.equals(event.eventType())) {
            String sql = "UPDATE payment_views SET status = 'FAILED', updated_at = ? WHERE payment_id = ?";
            jdbcTemplate.update(sql, Timestamp.from(event.occurredAt()), paymentId);
            System.out.println("READ MODEL: Payment " + paymentId + " is GEFAALD");
        }
    }


}

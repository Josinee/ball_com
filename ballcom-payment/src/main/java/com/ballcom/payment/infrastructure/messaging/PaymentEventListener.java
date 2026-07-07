package com.ballcom.payment.infrastructure.messaging;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        if (EventType.ORDER_PLACED.equals(event.eventType())) {

            Map<String, Object> payload = (Map<String, Object>) event.payload();

            String methodFromEvent = (String) payload.get("paymentMethod");
            PaymentMethod mappedMethod = PaymentMethod.valueOf(methodFromEvent.toUpperCase());

            var command = new ProcessPaymentCommand(
                UUID.fromString((String) payload.get("customerId")),
                event.aggregateId(), // De orderId
                new BigDecimal(payload.get("totalAmount").toString()),
                mappedMethod);

            // Stuur door naar de PaymentCommandHandler
            commandHandler.handle(command);

        }

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

        // FIX VOOR OPTIE 2: event.aggregateId() is NU het unieke paymentId!
        UUID paymentId = event.aggregateId(); 

        if (EventType.PAYMENT_INITIATED.equals(event.eventType())) {
            // Haal de orderId en overige info nu veilig uit de payload
            UUID orderId = UUID.fromString((String) payload.get("orderId"));
            UUID customerId = UUID.fromString((String) payload.get("customerId"));
            BigDecimal totalAmount = new BigDecimal(payload.get("total").toString());
            String paymentMethod = (String) payload.get("paymentMethod");

            String sql = """
                INSERT INTO payment_views (payment_id, order_id, customer_id, total_amount, payment_method, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            jdbcTemplate.update(sql, paymentId, orderId, customerId, totalAmount, paymentMethod, "INITIATED", Timestamp.from(event.occurredAt()));
            System.out.println("READ MODEL: Payment " + paymentId + " aangemaakt voor order " + orderId);
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

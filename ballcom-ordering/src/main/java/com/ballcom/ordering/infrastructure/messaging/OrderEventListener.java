package com.ballcom.ordering.infrastructure.messaging;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final JdbcTemplate jdbcTemplate;

    public OrderEventListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }

    @RabbitListener(queues = "ordering-payment-updates-queue")
    public void listenPaymentUpdates(GenericDomainEvent event) {
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if(rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + "al eerder verwerkt"); //TODO engels?
                return;
            }
            if(EventType.PAYMENT_COMPLETED.equals(event.eventType())) {
                Map<String, Object> payload = (Map<String, Object>) event.payload();
                String orderId = (String) payload.get("orderId");

                String sql = "UPDATE order_views SET payment_status = 'PAID' WHERE order_id = ?";
                jdbcTemplate.update(sql, orderId);
                System.out.println("READ MODEL: Order " + orderId + " gemarkeerd als BETAALD.");
            } 
            
            else if (EventType.PAYMENT_FAILED.equals(event.eventType())) {
                Map<String, Object> payload = (Map<String, Object>) event.payload();
                String orderId = (String) payload.get("orderId");

                String sql = "UPDATE order_views SET payment_status = 'PAYMENT_FAILED' WHERE order_id = ?";
                jdbcTemplate.update(sql, orderId);
                System.out.println("READ MODEL: Order " + orderId + " gemarkeerd als BETALING MISLUKT.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Deze methode luistert naar de queue
    @RabbitListener(queues = "ordering-readmodel-queue")
    public void consume(GenericDomainEvent event) {
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if (rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + " already processed. Skipping.");
                return;
            }
        
            System.out.println("CONSUMER: Event received in read model! Type: " + event.eventType());
            
            if (EventType.ORDER_PLACED.equals(event.eventType())) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) event.payload();

                    UUID orderId = event.aggregateId();
                    UUID customerId = UUID.fromString((String) payload.get("customerId"));
                    BigDecimal totalAmount = new BigDecimal(payload.get("totalAmount").toString());
                    
                    String orderStatus = "PLACED";
                    String paymentStatus = "PENDING";

                    String sql = """
                        INSERT INTO order_views (order_id, customer_id, total_amount, order_status, payment_status, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (order_id) DO UPDATE 
                        SET order_status = EXCLUDED.order_status, 
                            payment_status = EXCLUDED.payment_status, 
                            updated_at = EXCLUDED.updated_at;
                    """;

                    jdbcTemplate.update(sql, 
                        orderId, 
                        customerId, 
                        totalAmount, 
                        orderStatus,
                        paymentStatus,
                        Timestamp.from(event.occurredAt())
                    );

                    System.out.println("READ MODEL: Order " + orderId + " successfully saved to order_views.");
                } catch (Exception e) {
                    System.err.println("Error processing order event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in event consumer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
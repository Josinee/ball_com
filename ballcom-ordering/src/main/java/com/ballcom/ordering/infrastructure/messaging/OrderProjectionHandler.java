package com.ballcom.ordering.infrastructure.messaging;

import com.ballcom.shared.events.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderProjectionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OrderProjectionHandler(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // Deze methode luistert naar de queue
    @RabbitListener(queues = "ordering-readmodel-queue")
    public void consume(GenericDomainEvent event) {
        System.out.println("CONSUMER: Event ontvangen in de read-kant! Type: " + event.eventType());
        
        if ("OrderPlaced".equals(event.eventType())) {
            try {
                Map<String, Object> payload = (Map<String, Object>) event.payload();

                UUID orderId = event.aggregateId();
                UUID customerId = UUID.fromString((String) payload.get("customerId"));
                BigDecimal totalAmount = new BigDecimal(payload.get("totalAmount").toString());
                String status = "PLACED";

                String sql = """
                    INSERT INTO order_views (order_id, customer_id, total_amount, status, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (order_id) DO UPDATE 
                    SET status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;
                """;

                jdbcTemplate.update(sql, 
                    orderId, 
                    customerId, 
                    totalAmount, 
                    status, 
                    Timestamp.from(event.occurredAt())
                );

                System.out.println("READ MODEL: Order " + orderId + " succesvol opgeslagen in order_views");
            } catch (Exception e) {
                System.err.println("fout in verwerken van order " + e.getMessage());
                e.printStackTrace();
            }
            
            
        }
    }
}
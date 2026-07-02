package com.ballcom.customer.infrastructure.messaging;

import com.ballcom.customer.domain.valueobject.Address;
import com.ballcom.customer.domain.valueobject.EmailAddress;
import com.ballcom.shared.events.EventType;
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
public class CustomerProjectionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CustomerProjectionHandler(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // Deze methode luistert naar de queue
    @RabbitListener(queues = "customer-readmodel-queue")
    public void consume(GenericDomainEvent event) {
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if(rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + "al eerder verwerkt"); //TODO engels?
                return;
            }
        
            System.out.println("CONSUMER: Event ontvangen in de read-kant! Type: " + event.eventType()); // TODO engels?
            
            if (EventType.CUSTOMER_REGISTERED.equals(event.eventType())) {
                try {
                    Map<String, Object> payload = (Map<String, Object>) event.payload();

                    UUID customerId = event.aggregateId();
                    String name = (String) payload.get("name");
                    EmailAddress emailAddress = new EmailAddress((String) payload.get("emailAddress"));
                    Address address = new Address((String) payload.get("street"), (String) payload.get("houseNumber"),(String) payload.get("city"),(String) payload.get("zipCode"));

                    String sql = """
                        INSERT INTO customer_views (customer_id, name, email_address, address)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (customer_id) DO UPDATE 
                        SET status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;
                    """;

                    jdbcTemplate.update(sql, 
                        customerId,
                        name,
                        emailAddress,
                        address,
                        Timestamp.from(event.occurredAt())
                    );

                    System.out.println("READ MODEL: Customer " + customerId + " succesvol opgeslagen in customer_views");
                } catch (Exception e) {
                    System.err.println("fout in verwerken van customer " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }catch(Exception e) {
            System.err.println("fout in verwerken van customer " + e.getMessage());
            e.printStackTrace();
        }
            
    }
}
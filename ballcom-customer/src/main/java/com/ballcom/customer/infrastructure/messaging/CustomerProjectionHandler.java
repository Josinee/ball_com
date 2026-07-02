package com.ballcom.customer.infrastructure.messaging;

import com.ballcom.customer.domain.valueobject.Address;
import com.ballcom.customer.domain.valueobject.Email;
import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerProjectionHandler {

    private final JdbcTemplate jdbcTemplate;

    public CustomerProjectionHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
                Map<String, Object> payload = (Map<String, Object>) event.payload();

                UUID customerId = event.aggregateId();
                String name = (String) payload.get("name");
                String email = (String) payload.get("email");
                String street = (String) payload.get("street");
                String houseNumber = (String) payload.get("houseNumber");
                String city = (String) payload.get("city");
                String zipCode = (String) payload.get("zipCode");
                

                String sql = """
                    INSERT INTO customer_views (
                        customer_id,
                        name,
                        email,
                        street,
                        house_number,
                        city,
                        zip_code,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (customer_id) DO UPDATE
                    SET name = EXCLUDED.name,
                        email = EXCLUDED.email,
                        street = EXCLUDED.street,
                        house_number = EXCLUDED.house_number,
                        city = EXCLUDED.city,
                        zip_code = EXCLUDED.zip_code,
                        updated_at = EXCLUDED.updated_at
                """;

                jdbcTemplate.update(
                    sql,
                    customerId,
                    name,
                    email,
                    street,
                    houseNumber,
                    city,
                    zipCode,
                    Timestamp.from(event.occurredAt())

                );

                System.out.println("Customer view opgeslagen: " + customerId);
            }

        } catch (Exception e) {
            System.err.println("ERROR processing customer event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
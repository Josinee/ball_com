package com.ballcom.customer.infrastructure.messaging;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;

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

    @RabbitListener(queues = "customer-readmodel-queue")
    public void consume(GenericDomainEvent event) {
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if(rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + "al eerder verwerkt");
                return;
            }
                    
            if (EventType.CUSTOMER_REGISTERED.equals(event.eventType())) {
                Map<String, Object> payload = (Map<String, Object>) event.payload();

                UUID customerId = event.aggregateId();
                String companyName = (String) payload.get("companyName");
                String firstName = (String) payload.get("firstName");
                String lastName = (String) payload.get("lastName");
                String phoneNumber = (String) payload.get("phoneNumber");
                String street = (String) payload.get("street");
                String houseNumber = (String) payload.get("houseNumber");
                String city = (String) payload.get("city");
                String zipCode = (String) payload.get("zipCode");
                

                String sql = """
                    INSERT INTO customer_views (
                        customer_id,
                        company_name,
                        first_name,
                        last_name,
                        phone_number,
                        street,
                        house_number,
                        city,
                        zip_code,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (customer_id) DO UPDATE
                    SET company_name = EXCLUDED.company_name,
                        first_name = EXCLUDED.first_name,
                        last_name = EXCLUDED.last_name,
                        phone_number = EXCLUDED.phone_number,
                        street = EXCLUDED.street,
                        house_number = EXCLUDED.house_number,
                        city = EXCLUDED.city,
                        zip_code = EXCLUDED.zip_code,
                        updated_at = EXCLUDED.updated_at
                """;

                jdbcTemplate.update(
                    sql,
                    customerId,
                    companyName,
                    firstName,
                    lastName,
                    phoneNumber,
                    street,
                    houseNumber,
                    city,
                    zipCode,
                    Timestamp.from(event.occurredAt())

                );

                System.out.println("Customer opgeslagen: " + customerId);
            }

        } catch (Exception e) {
            System.err.println("Error processing customer event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
package com.ballcom.catalog.infrastructure.messaging;

import java.security.Timestamp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.ballcom.shared.events.GenericDomainEvent;

@Component
public class CatalogProjectHandler {
    private final JdbcTemplate jdbcTemplate;

    public CatalogProjectHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @RabbitListener(queues = "catalog-readmodel-queue")
    public void consume(GenericDomainEvent event) {
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if(rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + "al eerder verwerkt");
                return;
            }

            System.out.println("CATALOG: Event ontvangen in de read-kant! Type: " + event.eventType());

            //TODO: de dingen van catalogus die in de db moeten: naam, prijs etc.
        } catch (Exception e) {
            System.err.println("ERROR processing catalog event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

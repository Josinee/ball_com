package com.ballcom.catalog.infrastructure.messaging;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import com.ballcom.shared.events.EventType;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.ballcom.shared.events.GenericDomainEvent;

@Component
public class CatalogProjectionHandler {
    private final JdbcTemplate jdbcTemplate;

    public CatalogProjectionHandler(JdbcTemplate jdbcTemplate) {
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

            if(EventType.CATALOG_CREATED.equals(event.eventType())) {
                Map<String, Object> payload = (Map<String, Object>) event.payload();

                UUID catalogId = event.aggregateId();
                String itemName = (String) payload.get("itemName");
                String price = (String) payload.get("price");
                String description = (String) payload.get("description");
                String category = (String) payload.get("category");
                String availability = (String) payload.get("availability");
                String owner = (String) payload.get("owner");

                String sql = """
                INSERT INTO catalog_views (
                catalog_id,
                item_name,
                price,
                description,
                category,
                availability,
                owner,
                updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (catalog_id) DO UPDATE
                SET item_name = EXCLUDED.item_name,
                price = EXCLUDED.price,
                description = EXCLUDED.description,
                category = EXCLUDED.category,
                availability = EXCLUDED.availability,
                owner = EXCLUDED.owner,
                updated_at = EXCLUDED.updated_at
                """;

                jdbcTemplate.update(sql,
                    catalogId,
                    itemName,
                    price,
                    description,
                    category,
                    availability,
                    owner,
                    Timestamp.from(event.occurredAt())
                );

                System.out.println("Catalog view opgeslagen: " + catalogId);
            }

            //TODO: de dingen van catalogus die in de db moeten: naam, prijs etc.
        } catch (Exception e) {
            System.err.println("ERROR processing catalog event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

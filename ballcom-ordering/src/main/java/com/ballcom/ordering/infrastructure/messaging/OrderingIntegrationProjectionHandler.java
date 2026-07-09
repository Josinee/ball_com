package com.ballcom.ordering.infrastructure.messaging;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderingIntegrationProjectionHandler {

    private final JdbcTemplate jdbcTemplate;

    public OrderingIntegrationProjectionHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @RabbitListener(queues = "ordering-catalog-updates-queue")
    public void consumeCatalogEvent(GenericDomainEvent event) {
        if (!EventType.CATALOG_CREATED.equals(event.eventType())) {
            return;
        }

        Map<String, Object> payload = event.payload();

        UUID productId = event.aggregateId();
        String itemName = payload.get("itemName").toString();
        BigDecimal price = new BigDecimal(payload.get("price").toString());
        String category = payload.get("category").toString();
        String availability = payload.get("availability").toString();
        String owner = payload.get("owner").toString();

        String sql = """
            INSERT INTO ordering_catalog_items (
                product_id,
                item_name,
                price,
                category,
                availability,
                owner,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (product_id) DO UPDATE
            SET item_name = EXCLUDED.item_name,
                price = EXCLUDED.price,
                category = EXCLUDED.category,
                availability = EXCLUDED.availability,
                owner = EXCLUDED.owner,
                updated_at = EXCLUDED.updated_at
        """;

        jdbcTemplate.update(
                sql,
                productId,
                itemName,
                price,
                category,
                availability,
                owner,
                Timestamp.from(event.occurredAt())
        );

        System.out.println("ORDERING: Catalog item projection updated: " + productId);
    }

    @Transactional
    @RabbitListener(queues = "ordering-customer-updates-queue")
    public void consumeCustomerEvent(GenericDomainEvent event) {
        if (!EventType.CUSTOMER_REGISTERED.equals(event.eventType())) {
            return;
        }

        Map<String, Object> payload = event.payload();

        UUID customerId = event.aggregateId();

        String companyName = payload.get("companyName") != null
                ? payload.get("companyName").toString()
                : null;

        String firstName = payload.get("firstName") != null
                ? payload.get("firstName").toString()
                : null;

        String lastName = payload.get("lastName") != null
                ? payload.get("lastName").toString()
                : null;

        String sql = """
            INSERT INTO ordering_customers (
                customer_id,
                company_name,
                first_name,
                last_name,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (customer_id) DO UPDATE
            SET company_name = EXCLUDED.company_name,
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                updated_at = EXCLUDED.updated_at
        """;

        jdbcTemplate.update(
                sql,
                customerId,
                companyName,
                firstName,
                lastName,
                Timestamp.from(event.occurredAt())
        );

        System.out.println("ORDERING: Customer projection updated: " + customerId);
    }
}
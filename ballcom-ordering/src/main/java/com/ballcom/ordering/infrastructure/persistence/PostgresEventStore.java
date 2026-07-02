package com.ballcom.ordering.infrastructure.persistence;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import com.ballcom.shared.messaging.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public class PostgresEventStore implements EventStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    public PostgresEventStore(JdbcTemplate jdbcTemplate,
                              ObjectMapper objectMapper,
                              EventPublisher eventPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public void append(UUID aggregateId,
                       List<GenericDomainEvent> events,
                       long expectedVersion) {

        // 1. huidige versie ophalen
        long currentVersion = getCurrentVersion(aggregateId);

        // 2. concurrency check
        if (currentVersion != expectedVersion) {
            throw new RuntimeException(
                "Concurrency conflict: expected " + expectedVersion +
                " but was " + currentVersion
            );
        }

        long sequence = expectedVersion;

        for (GenericDomainEvent event : events) {

            sequence++;

            try {
                String jsonPayload = objectMapper.writeValueAsString(event.payload());

                String sql =
                    "INSERT INTO event_store " +
                    "(id, aggregate_id, aggregate_type, sequence_number, event_type, payload, occurred_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)";

                jdbcTemplate.update(
                    sql,
                    event.eventId(),
                    event.aggregateId(),
                    "Order",
                    sequence,
                    event.eventType().name(),
                    jsonPayload,
                    java.sql.Timestamp.from(event.occurredAt())
                );

                eventPublisher.publish(event);

            } catch (Exception e) {
                throw new RuntimeException("Error writing event to store", e);
            }
        }
    }


    private long getCurrentVersion(UUID aggregateId) {

        Long version = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(sequence_number), 0) FROM event_store WHERE aggregate_id = ?",
            Long.class,
            aggregateId
        );

        return version != null ? version : 0;
    }

    @Override
    public List<GenericDomainEvent> loadEvents(UUID aggregateId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadEvents'");
    }
}
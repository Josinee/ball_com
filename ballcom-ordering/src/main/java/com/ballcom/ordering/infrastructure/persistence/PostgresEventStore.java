package com.ballcom.ordering.infrastructure.persistence;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import com.ballcom.shared.messaging.EventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    public void append(UUID aggregateId, List<GenericDomainEvent> events, long expectedVersion) {
        //huidige versie ophalen
        long currentVersion = getCurrentVersion(aggregateId);

        if (currentVersion != expectedVersion) {
            throw new RuntimeException("Concurrency conflict: expected " + expectedVersion + " but was " + currentVersion);
        }


        for (GenericDomainEvent event : events) {
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
                    event.sequenceNumber(),
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
            "SELECT MAX(sequence_number) FROM event_store WHERE aggregate_id = ?",
            Long.class,
            aggregateId
        );

        return version != null ? version : -1;  
    }

    @Override
    public List<GenericDomainEvent> loadEvents(UUID aggregateId) {
        String sql = "SELECT id, aggregate_id, aggregate_type, sequence_number, event_type, occurred_at, payload " +
                     "FROM event_store " + "WHERE aggregate_id = ? " + "ORDER BY sequence_number ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                UUID eventId = UUID.fromString(rs.getString("id"));
                UUID aggId = UUID.fromString(rs.getString("aggregate_id"));
                String aggegateType = rs.getString("aggregate_type");
                long sequenceNumber = rs.getLong("sequence_number");
                EventType eventType = EventType.valueOf(rs.getString("event_type"));
                Instant occurredAt = rs.getTimestamp("occurred_at").toInstant();
                
                String payloadJson = rs.getString("payload");
                Map<String, Object> payload = objectMapper.readValue(
                    payloadJson, 
                    new TypeReference<Map<String, Object>>() {}
                );

                return new GenericDomainEvent(eventId, aggId, sequenceNumber, eventType, occurredAt, payload);
            } catch (Exception e) {
                throw new RuntimeException("Fout bij het omzetten van database row naar GenericDomainEvent", e);
            }
        }, aggregateId.toString());
    }

    @Override
    public List<GenericDomainEvent> loadEventsById(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadEventsById'");
    }
    
}
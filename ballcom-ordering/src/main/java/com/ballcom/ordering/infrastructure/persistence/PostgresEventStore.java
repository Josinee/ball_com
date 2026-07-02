package com.ballcom.ordering.infrastructure.persistence;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.messaging.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public class PostgresEventStore{

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper; // Spring Boot's ingebouwde JSON-vertaler
    private final EventPublisher eventPublisher;

    public PostgresEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, EventPublisher eventPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }


    @Transactional //dat ze of allemaal slagen of allemaal niet
    public void append(UUID aggregateId, List<GenericDomainEvent> events, long version) {
        for(GenericDomainEvent event : events) {
    
            try {
                //vertaal java event naar JSON
                String jsonPayload = objectMapper.writeValueAsString(event.payload());
                //sql query om event op te slaan in event store
                String sql = "INSERT INTO event_store (id, aggregate_id, aggregate_type, sequence_number, event_type, payload, occurred_at) VALUES (?, ?, 'Order', ?, ?, ?::jsonb, ?)";
                jdbcTemplate.update(sql, event.eventId(), event.aggregateId(), event.sequenceNumber(), event.eventType().name(), jsonPayload, java.sql.Timestamp.from(event.occurredAt()));

                eventPublisher.publish(event);
            } catch (DuplicateKeyException e) {
                // Als de combinatie van aggregateId en sequence_number al bestaat, gooit Postgres een Unique Constraint fout.
                throw new RuntimeException("Concurrency conflict! Versie " + event.sequenceNumber() + " voor order " + aggregateId + " bestaat al.", e);
                    
            } catch (Exception e) {

                //TODO andere exception uitleg
                throw new RuntimeException("fout bij wegschrijven naar Event Store " + e.getMessage());
                
            }
        }
    }

}
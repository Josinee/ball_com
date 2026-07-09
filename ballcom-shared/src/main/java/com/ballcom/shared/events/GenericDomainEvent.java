package com.ballcom.shared.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// message envelope pattern. seperates metadata and raw payload. rabbitmq heeft zo geen informatie nodig over wat het is, is gewoon altijd deze vorm
public record GenericDomainEvent(
    UUID eventId,
    UUID aggregateId,
    long sequenceNumber,
    EventType eventType,
    Instant occurredAt,
    Map<String, Object> payload
) {}
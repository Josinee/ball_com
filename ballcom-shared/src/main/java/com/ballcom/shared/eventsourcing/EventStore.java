package com.ballcom.shared.eventsourcing;

import java.util.List;
import java.util.UUID;

import com.ballcom.shared.events.GenericDomainEvent;
// poort naar database voor event sourcing
public interface EventStore {
    void append(UUID aggregateId, List<GenericDomainEvent> events, long sequenceNumber);
    List<GenericDomainEvent> loadEvents(UUID aggregateId);
    List<GenericDomainEvent> loadEventsById(UUID id);

    
}

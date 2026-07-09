package com.ballcom.shared.eventsourcing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ballcom.shared.events.GenericDomainEvent;
public abstract class AggregateRoot {
    protected UUID id;
    protected long sequenceNumber =-1;
    private final List<GenericDomainEvent> uncommitedEvents = new ArrayList<>();
    
    public UUID getId(){ return id;}
    public long getSequenceNumber() { return sequenceNumber;}

    public List<GenericDomainEvent> getUncommitedEvents() {return List.copyOf(uncommitedEvents);}

    public void clearUncommitedEvents() {
        uncommitedEvents.clear();
    }

    protected void raiseEvent(GenericDomainEvent event) {
        apply(event);
        this.sequenceNumber = event.sequenceNumber();
        uncommitedEvents.add(event);
    }

    public void loadFromHistory(Iterable<GenericDomainEvent> history) {
        for (GenericDomainEvent event : history) {
            apply(event);
            this.sequenceNumber = event.sequenceNumber();;
            
        }
    }

    public long getExpectedVersion() {
        return this.sequenceNumber - this.uncommitedEvents.size();
    }
    // elke aggregate implementeerd deze methode om zijn state bij te werken
    protected abstract void apply(GenericDomainEvent event);
}
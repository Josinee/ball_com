package com.ballcom.shared.eventsourcing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ballcom.shared.events.GenericDomainEvent;
// houd interne status en de uncommited events lijst bij
public abstract class AggregateRoot {
    protected UUID id;
    protected long sequenceNumber =-1; //-1 betekent een nieuwe aggregate
    private final List<GenericDomainEvent> uncommitedEvents = new ArrayList<>();
    
    public UUID getId(){ return id;}
    public long getSequenceNumber() { return sequenceNumber;}

    public List<GenericDomainEvent> getUncommitedEvents() {return List.copyOf(uncommitedEvents);}

    public void clearUncommitedEvents() {
        uncommitedEvents.clear();
    }

    // wordt aangeroepen bij nieuwe mutaties
    protected void raiseEvent(GenericDomainEvent event) {
        apply(event);
        this.sequenceNumber = event.sequenceNumber();
        uncommitedEvents.add(event);
    }

    // wordt aangeroepen bij het herladen van history uit de database
    public void loadFromHistory(Iterable<GenericDomainEvent> history) {
        for (GenericDomainEvent event : history) {
            apply(event);
            this.sequenceNumber = event.sequenceNumber();;
            
        }
    }
    // elke aggregate implementeerd deze methode om zijn state bij te werken
    protected abstract void apply(GenericDomainEvent event);
}
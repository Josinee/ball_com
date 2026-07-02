package com.ballcom.customer.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ballcom.customer.domain.valueobject.Address;
import com.ballcom.customer.domain.valueobject.Email;
import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;

public class CustomerAggregate extends AggregateRoot{

    private UUID id;
    private String name;
    private Email email;
    private Address address;

    public CustomerAggregate() {}
    
    //business logica, als alles mag worden er geen velden veranderd, alleen event aangemaakt
    public static CustomerAggregate register(String name, Email email, Address address) {
        UUID customerId = UUID.randomUUID();
        CustomerAggregate customer = new CustomerAggregate();
        customer.id = customerId;
        Map<String, Object> payload = Map.of(
            "name", name, 
            "email", email.value(),
            "street", address.street(),
            "houseNumber", address.houseNumber(),
            "city", address.city(),
            "zipCode", address.zipCode()
        );

     
        
        GenericDomainEvent event = new GenericDomainEvent(UUID.randomUUID(), customerId, 0, EventType.CUSTOMER_REGISTERED, Instant.now(), payload);
        customer.raiseEvent(event);
        System.out.println("in register in aggregate " + customer.id);
        return customer;
    }



    //wordt aangeroepen door raiseEvent, veranderd interne velden op basis van het event
    @Override
    protected void apply(GenericDomainEvent event) {
        if(EventType.CUSTOMER_REGISTERED.equals(event.eventType())) {
            this.id = event.aggregateId();
            Map<String, Object> payload = event.payload();
            this.name = (String) payload.get("name");
            this.email = new Email((String) payload.get("email"));
            this.address = new Address(
                (String) payload.get("street"),
                (String) payload.get("houseNumber"),
                (String) payload.get("city"),
                (String) payload.get("zipCode")
            );
        }
        this.sequenceNumber = event.sequenceNumber();
    }

    public UUID getId() {
        return this.id;
    }


    
}

package com.ballcom.customer.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ballcom.customer.domain.valueobject.Address;
import com.ballcom.customer.domain.valueobject.PhoneNumber;
import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;

public class CustomerAggregate extends AggregateRoot{

    private UUID id;
    private String companyName;
    private String firstName;
    private String lastName;
    private PhoneNumber phoneNumber;
    private Address address;

    public CustomerAggregate() {}
    
    //business logica, als alles mag worden er geen velden veranderd, alleen event aangemaakt
    public static CustomerAggregate register(String companyName, String firstName, String lastName, PhoneNumber phoneNumber, Address address) {
        UUID customerId = UUID.randomUUID();
        CustomerAggregate customer = new CustomerAggregate();
        customer.id = customerId;
        Map<String, Object> payload = Map.of(
            "companyName", companyName,
            "firstName", firstName, 
            "lastName", lastName,
            "phoneNumber", phoneNumber.value(),
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
            this.companyName = (String) payload.get("companyName");
            this.firstName = (String) payload.get("firstName");
            this.lastName = (String) payload.get("lastName");
            this.phoneNumber = new PhoneNumber((String) payload.get("phoneNumber"));
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

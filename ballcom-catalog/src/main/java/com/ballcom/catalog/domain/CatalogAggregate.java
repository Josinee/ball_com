package com.ballcom.catalog.domain;

//TODO: AANPASSEN

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;


public class CatalogAggregate extends AggregateRoot{


    private UUID id;
    private String itemName;
    private String price;
    private String description;
    private String category;
    private String availability;
    private String owner;



    public CatalogAggregate() {}
    
    //business logica, als alles mag worden er geen velden veranderd, alleen event aangemaakt
    public static CatalogAggregate create(String itemName, String price, String category, String description, String availability, String owner) {
        UUID catalogId = UUID.randomUUID();
        CatalogAggregate catalog = new CatalogAggregate();
        catalog.id = catalogId;
        Map<String, Object> payload = Map.of(
            "itemName", itemName,
            "price", price,
            "category", category, 
            "description", description,
            "availabilty", availability,
            "owner", owner
        );

     
        
        GenericDomainEvent event = new GenericDomainEvent(UUID.randomUUID(), catalogId, 0, EventType.CATALOG_CREATED, Instant.now(), payload);
        catalog.raiseEvent(event);
        System.out.println("in create in aggregate " + catalog.id);
        return catalog;
    }



    //wordt aangeroepen door raiseEvent, veranderd interne velden op basis van het event
   // @Override
    protected void apply(GenericDomainEvent event) {
        if(EventType.CATALOG_CREATED.equals(event.eventType())) {
            this.id = event.aggregateId();
            Map<String, Object> payload = event.payload();
            this.itemName = (String) payload.get("itemName");
            this.price = (String) payload.get("price");
            this.category = (String) payload.get("category");
            this.description = (String) payload.get("description");
            this.availability = (String) payload.get("availability");
            this.owner = (String) payload.get("owner");
        }
        this.sequenceNumber = event.sequenceNumber();
    }

    public UUID getId() {
        return this.id;
    }


    
}



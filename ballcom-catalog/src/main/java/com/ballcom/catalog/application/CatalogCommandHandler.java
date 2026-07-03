package com.ballcom.catalog.application;

import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shared.eventsourcing.EventStore;

//TODO businessrules hier
public class CatalogCommandHandler {
    
    private final EventStore eventStore;

    public CatalogCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public UUID handle(CreateCatalogCommand command){
        things

        CatalogAggregate catalog = CatalogAggregate.create(command.productName(), command.);

        eventStore.append(catalog.getId(), )
    }
}

package com.ballcom.catalog.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shared.eventsourcing.EventStore;
import com.ballcom.catalog.application.commands.CreateCatalogCommand;
import com.ballcom.catalog.domain.CatalogAggregate;

@Service
public class CatalogCommandHandler {
    
    private final EventStore eventStore;

    public CatalogCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public UUID handle(CreateCatalogCommand command){
         
        CatalogAggregate catalog = CatalogAggregate.create(command.itemName(), command.price(), command.category(), command.description(), command.availability(), command.owner());

        eventStore.append(catalog.getId(), catalog.getUncommitedEvents(), 0);
        catalog.clearUncommitedEvents();

        return catalog.getId();
        }
}

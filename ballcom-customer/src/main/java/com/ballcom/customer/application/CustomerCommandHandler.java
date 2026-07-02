package com.ballcom.customer.application;

import com.ballcom.customer.domain.CustomerAggregate;
import com.ballcom.customer.domain.valueobject.Address;
import com.ballcom.customer.domain.valueobject.Email;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.shared.eventsourcing.EventStore;

@Service
public class CustomerCommandHandler {

    private final EventStore eventStore;

    public CustomerCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public UUID handle(RegisterCustomerCommand command){
        Email email = new Email(command.email());
        Address address = new Address(command.street(), command.houseNumber(), command.city(), command.zipCode());
        
        CustomerAggregate customer = CustomerAggregate.register(command.name(), email, address);

        eventStore.append(customer.getId(), customer.getUncommitedEvents(), 0);
        customer.clearUncommitedEvents();

        return customer.getId();
        }
}

package com.ballcom.customer.application;

import com.ballcom.customer.application.commands.RegisterCustomerCommand;
import com.ballcom.customer.domain.CustomerAggregate;
import com.ballcom.customer.domain.valueobject.Address;
import com.ballcom.customer.domain.valueobject.PhoneNumber;

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
        PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber());
        Address address = new Address(command.street(), command.houseNumber(), command.city(), command.zipCode());
        
        CustomerAggregate customer = CustomerAggregate.register(command.companyName(), command.firstName(), command.lastName(), phoneNumber, address);

        eventStore.append(customer.getId(), customer.getUncommitedEvents(), customer.getExpectedVersion());
        customer.clearUncommitedEvents();

        return customer.getId();
        }
}

package com.ballcom.ordering.commands;


import com.ballcom.ordering.domain.OrderAggregate;
import com.ballcom.ordering.domain.OrderItem;
import com.ballcom.ordering.infrastructure.persistence.PostgresEventStore;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
public class OrderCommandHandler {
    private final PostgresEventStore eventStore;

    public OrderCommandHandler(PostgresEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public UUID handle(PlaceOrderCommand command) {
        //krijg de items uit de command, vertaal het naar domeinobjecten
        List<OrderItem> items = command.items();

        //omdat het een nieuwe order is, maakt hij een nieuwe OrderAggregate, aggregate slaat de event intern op
        OrderAggregate order = OrderAggregate.place(command.customerId(), items);

        //sla event op in eventstore
        eventStore.append(order.getId(), order.getUncommitedEvents(), order.getVersion());
        //publiceer een message dat het event heeft plaatsgevonden
        order.clearUncommitedEvents();
        return order.getId();
    }
    
}
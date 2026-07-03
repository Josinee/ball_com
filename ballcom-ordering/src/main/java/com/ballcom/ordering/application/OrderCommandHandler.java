package com.ballcom.ordering.application;


import com.ballcom.ordering.domain.OrderAggregate;
import com.ballcom.ordering.domain.OrderItem;
import com.ballcom.shared.eventsourcing.EventStore;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Component
public class OrderCommandHandler {
    private final EventStore eventStore;

    public OrderCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public UUID handle(PlaceOrderCommand command) {
        //krijg de items uit de command, vertaal het naar domeinobjecten
        List<OrderItem> items = command.items().stream()
            .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPrice())) // HIER pas het domein-object maken!
            .toList();

        //omdat het een nieuwe order is, maakt hij een nieuwe OrderAggregate, aggregate slaat de event intern op
        OrderAggregate order = OrderAggregate.place(command.customerId(), items, command.paymentMethod());


        //sla event op in eventstore
        eventStore.append(order.getId(), order.getUncommitedEvents(), order.getSequenceNumber());
        //publiceer een message dat het event heeft plaatsgevonden
        order.clearUncommitedEvents();
        return order.getId();
    }
    
}
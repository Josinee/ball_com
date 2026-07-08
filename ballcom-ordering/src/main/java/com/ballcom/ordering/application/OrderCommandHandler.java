package com.ballcom.ordering.application;


import com.ballcom.ordering.application.commands.ConfirmOrderPaymentCommand;
import com.ballcom.ordering.application.commands.PlaceOrderCommand;
import com.ballcom.ordering.domain.OrderAggregate;
import com.ballcom.ordering.domain.OrderItem;
import com.ballcom.ordering.infrastructure.catalog.CatalogClient;
import com.ballcom.ordering.infrastructure.catalog.CatalogItemResponse;
import com.ballcom.ordering.infrastructure.customer.CustomerClient;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Component
public class OrderCommandHandler {
    private final EventStore eventStore;
    private final CatalogClient catalogClient;
    private final CustomerClient customerClient;

    public OrderCommandHandler(
            EventStore eventStore,
            CatalogClient catalogClient,
            CustomerClient customerClient
    ) {
        this.eventStore = eventStore;
        this.catalogClient = catalogClient;
        this.customerClient = customerClient;
    }

    @Transactional
    public UUID handle(PlaceOrderCommand command) {
        if (command.customerId() == null) {
            throw new IllegalArgumentException("CustomerId is required");
        }

        if (!customerClient.customerExists(command.customerId())) {
            throw new IllegalArgumentException("Customer does not exist: " + command.customerId());
        }

        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least 1 item");
        }

        if (command.items().size() > 20) {
            throw new IllegalArgumentException("An order cannot contain more than 20 items");
        }


        //krijg de items uit de command, vertaal het naar domeinobjecten
        List<OrderItem> items = command.items().stream()
            .map(i -> {
                    if (i.productId() == null) {
                        throw new IllegalArgumentException("ProductId is required");
                    }

                    if (i.quantity() <= 0) {
                        throw new IllegalArgumentException("Quantity must be greater than zero");
                    }

                    CatalogItemResponse catalogItem = catalogClient.getCatalogItem(i.productId());

                    if (!"IN_STOCK".equalsIgnoreCase(catalogItem.availability())) {
                        throw new IllegalArgumentException("Product is not in stock: " + i.productId());
                    }

                    BigDecimal catalogPrice = new BigDecimal(catalogItem.price());

                    return new OrderItem(
                            catalogItem.catalogId(),
                            i.quantity(),
                            catalogPrice
                    );
                })
                .toList();

        //omdat het een nieuwe order is, maakt hij een nieuwe OrderAggregate, aggregate slaat de event intern op
        OrderAggregate order = OrderAggregate.place(command.customerId(), items, command.paymentMethod());


        //sla event op in eventstore
        eventStore.append(order.getId(), order.getUncommitedEvents(), order.getExpectedVersion());
        //publiceer een message dat het event heeft plaatsgevonden
        order.clearUncommitedEvents();
        return order.getId();
    }

    @Transactional
    public UUID handle(ConfirmOrderPaymentCommand command) {
        //haal events op uit eventstore
        List<GenericDomainEvent> history = eventStore.loadEvents(command.orderId());
        // reconstruct aggregate uit de history
        OrderAggregate order = new OrderAggregate();
        order.loadFromHistory(history);

        order.confirmPayment();
        //sla nieuwe event van confirm payment op
        eventStore.append(order.getId(), order.getUncommitedEvents(), order.getExpectedVersion());
        order.clearUncommitedEvents();
        return order.getId();

    }
    
}
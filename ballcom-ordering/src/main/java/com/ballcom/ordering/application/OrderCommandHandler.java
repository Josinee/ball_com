package com.ballcom.ordering.application;

import com.ballcom.ordering.application.commands.ConfirmOrderPaymentCommand;
import com.ballcom.ordering.application.commands.PlaceOrderCommand;
import com.ballcom.ordering.domain.OrderAggregate;
import com.ballcom.ordering.domain.OrderItem;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class OrderCommandHandler {
    private final EventStore eventStore;
    private final JdbcTemplate jdbcTemplate;

    public OrderCommandHandler(EventStore eventStore, JdbcTemplate jdbcTemplate) {
        this.eventStore = eventStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID handle(PlaceOrderCommand command) {
        if (command.customerId() == null) {
            throw new IllegalArgumentException("CustomerId is required");
        }

        if (!customerExistsLocally(command.customerId())) {
            throw new IllegalArgumentException(
                    "Customer is not known in Ordering yet: " + command.customerId()
            );
        }

        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least 1 item");
        }

        if (command.items().size() > 20) {
            throw new IllegalArgumentException("An order cannot contain more than 20 items");
        }

        List<OrderItem> items = command.items().stream()
                .map(i -> {
                    if (i.productId() == null) {
                        throw new IllegalArgumentException("ProductId is required");
                    }

                    if (i.quantity() <= 0) {
                        throw new IllegalArgumentException("Quantity must be greater than zero");
                    }

                    CatalogSnapshot product = getProductSnapshot(i.productId());

                    if (!"IN_STOCK".equalsIgnoreCase(product.availability())) {
                        throw new IllegalArgumentException(
                                "Product is not in stock: " + i.productId()
                        );
                    }

                    return new OrderItem(
                            product.productId(),
                            i.quantity(),
                            product.price()
                    );
                })
                .toList();

        OrderAggregate order = OrderAggregate.place(
                command.customerId(),
                items,
                command.paymentMethod()
        );

        eventStore.append(
                order.getId(),
                order.getUncommitedEvents(),
                order.getExpectedVersion()
        );

        order.clearUncommitedEvents();

        return order.getId();
    }

    private boolean customerExistsLocally(UUID customerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ordering_customers WHERE customer_id = ?",
                Integer.class,
                customerId
        );

        return count != null && count > 0;
    }

    private CatalogSnapshot getProductSnapshot(UUID productId) {
        String sql = """
            SELECT product_id, price, availability
            FROM ordering_catalog_items
            WHERE product_id = ?
        """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException(
                        "Product is not known in Ordering yet: " + productId
                );
            }

            return new CatalogSnapshot(
                    rs.getObject("product_id", UUID.class),
                    rs.getBigDecimal("price"),
                    rs.getString("availability")
            );
        }, productId);
    }

    private record CatalogSnapshot(
            UUID productId,
            BigDecimal price,
            String availability
    ) {}

    @Transactional
    public UUID handle(ConfirmOrderPaymentCommand command) {
        List<GenericDomainEvent> history = eventStore.loadEvents(command.orderId());

        OrderAggregate order = new OrderAggregate();
        order.loadFromHistory(history);

        order.confirmPayment();

        eventStore.append(
                order.getId(),
                order.getUncommitedEvents(),
                order.getExpectedVersion()
        );

        order.clearUncommitedEvents();

        return order.getId();
    }
}
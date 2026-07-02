package com.ballcom.ordering.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (in the hexagonal sense): the domain only knows this interface, never PostgresEventStore directly.
 */
public interface OrderRepository {
    void save(OrderAggregate order);
    Optional<OrderAggregate> findById(UUID orderId);
}

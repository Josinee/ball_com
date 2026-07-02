// package com.ballcom.ordering.domain;
// import java.math.BigDecimal;
// import java.time.Instant;
// import java.util.List;
// import java.util.UUID;

// import com.ballcom.shared.events.DomainEvent;

// // Dit is het pure contract/briefje dat over RabbitMQ gaat
// public record OrderPlacedEvent(UUID eventId, UUID aggregateId, Instant occurredAt, UUID customerId, List<OrderItem> items, BigDecimal totalAmount) implements DomainEvent {

//     public OrderPlacedEvent(UUID aggregateId, UUID customerId, List<OrderItem> items, BigDecimal totalAmount) {
//         // Hier roep je de standaard constructor aan en vul je de infra-velden zelf in
//         this(UUID.randomUUID(), aggregateId, Instant.now(), customerId, items, totalAmount);
//     }
// }
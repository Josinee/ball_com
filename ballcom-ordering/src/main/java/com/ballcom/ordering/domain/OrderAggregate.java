package com.ballcom.ordering.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;

public class OrderAggregate extends AggregateRoot {

    private UUID customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;

    public OrderAggregate() {}

    public static OrderAggregate place(UUID customerId, List<OrderItem> items, String paymentMethod) {
        if (items == null || items.isEmpty() || items.size() > 20) {
            throw new IllegalArgumentException("An order must contain between 1 and 20 items");
        }

        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderAggregate order = new OrderAggregate();
        UUID orderId = UUID.randomUUID();

        Map<String, Object> payload = Map.of(
            "customerId", customerId.toString(), 
            "items", items, 
            "paymentMethod", paymentMethod,
            "totalAmount", total.toString()
        );

        long nextSequence = order.getSequenceNumber() + 1;
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(), 
            orderId, 
            nextSequence, 
            EventType.ORDER_PLACED, 
            Instant.now(), 
            payload
        );

        order.raiseEvent(event); 

        return order;
    }

    @Override
    protected void apply(GenericDomainEvent event) {
        if (EventType.ORDER_PLACED.equals(event.eventType())) {
            this.id = event.aggregateId();
            
            Map<String, Object> data = event.payload();
            
            this.customerId = UUID.fromString((String) data.get("customerId"));
            this.items = (List<OrderItem>) data.get("items");
            this.totalAmount = new BigDecimal((String) data.get("totalAmount"));
            this.orderStatus = OrderStatus.PLACED;
            this.paymentStatus = PaymentStatus.UNPAID;
        }
        else if (EventType.PAYMENT_COMPLETED.equals(event.eventType())) {
            this.paymentStatus = PaymentStatus.PAID;
        }
    }


    public void confirmPayment() {
        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm payment for a cancelled order");
        }
        if (this.paymentStatus == PaymentStatus.PAID) return;

        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),
            this.getId(), 
            this.getSequenceNumber() + 1,
            EventType.PAYMENT_COMPLETED, 
            Instant.now(),
            Map.of("paymentStatus", "PAID")
        );
        
        this.raiseEvent(event); 
    }
    public UUID getId() { return this.id; }
    public UUID getCustomerId() { return this.customerId; }
    public List<OrderItem> getItems() { return this.items; }
    public BigDecimal getTotalAmount() { return this.totalAmount; }

}
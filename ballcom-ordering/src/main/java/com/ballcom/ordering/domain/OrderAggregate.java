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

    // Deze velden worden ingevuld DOOR het event in de apply-methode
    private UUID customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;

    // Verplichte lege constructor voor JPA/Jackson/Frameworks
    public OrderAggregate() {}

    // De statische fabrieksmethode om een order te plaatsen
    public static OrderAggregate place(UUID customerId, List<OrderItem> items, String paymentMethod) {
        // Business regels valideren
        if (items == null || items.isEmpty() || items.size() > 20) {
            throw new IllegalArgumentException("An order must contain between 1 and 20 items");
        }

        // Bereken totale prijs
        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderAggregate order = new OrderAggregate();
        UUID orderId = UUID.randomUUID();

        // Bouw de flexibele payload map 
        Map<String, Object> payload = Map.of(
            "customerId", customerId.toString(), 
            "items", items, 
            "paymentMethod", paymentMethod,
            "totalAmount", total.toString()
        );

        long nextSequence = order.getSequenceNumber() + 1;
        // Maak het generieke event aan (Sequence/versie is 1 bij een nieuwe order)
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(), 
            orderId, 
            nextSequence, 
            EventType.ORDER_PLACED, 
            Instant.now(), 
            payload
        );

        // HIER GEBEURT HET: Je raist het event via de base class (AggregateRoot)!
        // Let op: afhankelijk van je AggregateRoot base class heet dit 'raiseEvent(event)' of 'apply(event)'.
        order.raiseEvent(event); 

        return order;
    }

    //wordt aangeroepen door raiseEvent, veranderd interne velden op basis van het event
    @Override
    protected void apply(GenericDomainEvent event) {
        if (EventType.ORDER_PLACED.equals(event.eventType())) {
            this.id = event.aggregateId();
            
            Map<String, Object> data = event.payload();
            
            // Haal de data veilig uit de map met de juiste type-conversies
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
            this.getId(), // Ons aggregate ID
            this.getSequenceNumber() + 1, // Volgende versie
            EventType.PAYMENT_COMPLETED, 
            Instant.now(),
            Map.of("paymentStatus", "PAID")
        );
        
        this.raiseEvent(event); // Dit triggert apply() én zet hem in uncommitedEvents!
    }
    // Getters
    public UUID getId() { return this.id; }
    public UUID getCustomerId() { return this.customerId; }
    public List<OrderItem> getItems() { return this.items; }
    public BigDecimal getTotalAmount() { return this.totalAmount; }

}
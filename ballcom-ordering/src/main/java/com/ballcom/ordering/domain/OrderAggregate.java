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
    private UUID id; 
    private UUID customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;

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
            this.id = event.aggregateId(); // Pak het ID uit de buitenkant van de enveloppe
            
            Map<String, Object> data = event.payload();
            
            // Haal de data veilig uit de map met de juiste type-conversies
            this.customerId = UUID.fromString((String) data.get("customerId"));
            this.items = (List<OrderItem>) data.get("items");
            this.totalAmount = new BigDecimal((String) data.get("totalAmount"));
            this.status = OrderStatus.PLACED;
        }
        this.sequenceNumber = event.sequenceNumber();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
}
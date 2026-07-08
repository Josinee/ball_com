package com.ballcom.shipment.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.messaging.EventPublisher;

@Component
public class ShipmentEventPublisher implements EventPublisher{
    private final RabbitTemplate rabbitTemplate;


    public ShipmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(GenericDomainEvent event) { 
    
        System.out.println("=== TRANSPORT START ===");
        System.out.println("RABBITMQ: Poging tot verzenden... EventType: " + event.eventType()
                + " naar exchange: shipment.exchange");
        String routingKey = "shipment." + event.eventType().name().toLowerCase().replace("_", ".");
        try {
            rabbitTemplate.convertAndSend("shipment.exchange", routingKey, event);
            System.out.println("RABBITMQ: Succesvol gepubliceerd! RoutingKey: " + "shipment." + event.eventType().name().toLowerCase().replace("_", "."));
        } catch (Exception e) {
            System.err.println("RABBITMQ FOUT: Verzenden mislukt! " + e.getMessage());
        }
        System.out.println("=== TRANSPORT EIND ===");
    }
    }
    // published een SHIPPING_COST_CALCULATED waar payment naar luistert, maakt daarna een paymentaggregate aan

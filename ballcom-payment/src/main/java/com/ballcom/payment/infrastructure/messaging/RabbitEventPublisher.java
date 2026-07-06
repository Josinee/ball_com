package com.ballcom.payment.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.messaging.EventPublisher;

@Component
public class RabbitEventPublisher implements EventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) { 
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(GenericDomainEvent event) {
        // genereert specifieke routing key
        String routingKey = "payment." + event.eventType();

        System.out.println("=== TRANSPORT START ===");
        System.out.println("RABBITMQ: Poging tot verzenden... EventType: " + event.eventType()
                + " naar exchange: payment.exchange");

        try {
            rabbitTemplate.convertAndSend("payment.exchange", routingKey, event);
            System.out.println("RABBITMQ: Succesvol gepubliceerd! RoutingKey: " + routingKey);
        } catch (Exception e) {
            System.err.println("RABBITMQ FOUT: Verzenden mislukt! " + e.getMessage());
        }
        System.out.println("=== TRANSPORT EIND ===");
    }

}
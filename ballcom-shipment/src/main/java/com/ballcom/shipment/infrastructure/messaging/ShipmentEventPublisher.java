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
            String routingKey = "shipment." + event.eventType().name().toLowerCase().replace("_", ".");
        try {
            rabbitTemplate.convertAndSend("shipment.exchange", routingKey, event);
        } catch (Exception e) {
            System.err.println("RABBITMQ FOUT " + e.getMessage());
        }
    }
    }
    // published een SHIPPING_COST_CALCULATED waar payment naar luistert, maakt daarna een paymentaggregate aan

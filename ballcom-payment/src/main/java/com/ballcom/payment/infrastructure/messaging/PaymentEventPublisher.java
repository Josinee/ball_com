package com.ballcom.payment.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.messaging.EventPublisher;

@Component
public class PaymentEventPublisher implements EventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) { 
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(GenericDomainEvent event) {
        // genereert specifieke routing key
        String routingKey = "payment." + event.eventType();
        try {
            rabbitTemplate.convertAndSend("payment.exchange", routingKey, event);
        } catch (Exception e) {
            System.err.println("RABBITMQ FOUT " + e.getMessage());
        }
    }

}
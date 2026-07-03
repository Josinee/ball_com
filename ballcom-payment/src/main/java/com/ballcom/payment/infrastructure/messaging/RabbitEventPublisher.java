package com.ballcom.payment.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.messaging.EventPublisher;



@Component
public class RabbitEventPublisher implements EventPublisher{
    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    //wacht tot de database commit klaar is, stuurt daarna pas naar rabbit
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(GenericDomainEvent event) {
        //genereert specifieke routing key
        String routingKey = "payment." + event.eventType();
        
        rabbitTemplate.convertAndSend("payment.exchange", routingKey, event);
        System.out.println("RABBITMQ: Event gepubliceerd met routing key " + routingKey);
    }
}
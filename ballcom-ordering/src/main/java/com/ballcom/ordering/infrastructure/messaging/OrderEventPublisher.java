package com.ballcom.ordering.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.messaging.EventPublisher;

@Component
public class OrderEventPublisher implements EventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // wacht tot de database commit klaar is, stuurt daarna pas naar rabbit

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(GenericDomainEvent event) {
        // genereert specifieke routing key

        try {
            rabbitTemplate.convertAndSend("order.exchange", "order.placed", event);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
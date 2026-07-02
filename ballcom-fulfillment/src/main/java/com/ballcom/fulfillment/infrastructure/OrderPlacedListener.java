package com.ballcom.fulfillment.infrastructure;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ballcom.shared.events.GenericDomainEvent;

@Component
public class OrderPlacedListener {

    // Luister direct naar de queue. Spring vertaalt de JSON automatisch terug naar je Record.
    @RabbitListener(queues = "payment.queue")
    public void handleOrderPlaced(GenericDomainEvent event) {
        
        // Gewoon simpel verwerken
        System.out.println("PAYMENT: Bericht ontvangen voor order: " + event.aggregateId() + " van klant: " + event.payload());
    }
}
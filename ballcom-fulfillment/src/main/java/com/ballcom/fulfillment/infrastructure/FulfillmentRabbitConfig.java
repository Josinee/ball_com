package com.ballcom.fulfillment.infrastructure;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FulfillmentRabbitConfig {

    // 1. Maak een eigen, duurzame queue voor payment
    @Bean
    public Queue paymentQueue() {
        return new Queue("payment.queue", true);
    }

    // 2. Koppel de queue aan de topic exchange met een wildcard (*)
    @Bean
    public Binding binding(Queue paymentQueue) {
        return BindingBuilder.bind(paymentQueue)
                .to(new TopicExchange("order.exchange", true, false))
                .with("order.*"); // DIT IS HET TOPIC PATTERN! Vangt "order.orderplacedevent", "order.ordercancelledevent", etc.
    }
}
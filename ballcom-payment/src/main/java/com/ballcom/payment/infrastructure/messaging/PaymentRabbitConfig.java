package com.ballcom.payment.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitConfig {

    @Bean
    public Queue orderPlacedQueue() {
        return new Queue("payment-order-placed-queue", true);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange", true, false);
    }

    @Bean
    public Binding orderPlacedBinding(Queue orderPlacedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(orderExchange).with("order.placed");
    }



    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange", true, false);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue("payment-readmodel-queue", true);
    }

    @Bean
    public Binding customerBinding(Queue paymentQueue, TopicExchange paymentExchange) {

        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with("payment.#");
    }
}
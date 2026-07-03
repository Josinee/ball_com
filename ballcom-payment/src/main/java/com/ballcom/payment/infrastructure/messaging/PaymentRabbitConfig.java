package com.ballcom.payment.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitConfig {

    //maakt topic aan waaraan listeners kunnen subscriben
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange", true, false);
    }

    @Bean
    public Queue customerQueue() {
        return new Queue("payment-readmodel-queue", true);//durable true = overleefd herstart van rabbitmq
    }

    @Bean
    public Binding customerBinding(Queue paymentQueue, TopicExchange paymentExchange) { //TODO wat is dit wat doet het
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with("payment.#");
    }
}

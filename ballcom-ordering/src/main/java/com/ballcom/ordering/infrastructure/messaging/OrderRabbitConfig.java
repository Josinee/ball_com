package com.ballcom.ordering.infrastructure.messaging;



import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderRabbitConfig {

    //maakt topic aan waaraan listeners kunnen subscriben
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange", true, false);
    }

    @Bean
    public Queue orderQueue() {
        return new Queue("ordering-readmodel-queue", true);//durable true = overleefd herstart van rabbitmq
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) { //TODO wat is dit wat doet het
        return BindingBuilder.bind(orderQueue).to(orderExchange).with("order.#");
    }


    @Bean
    public Queue orderPaymentEventsQueue() {
        return new Queue("ordering-payment-updates-queue", true);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange", true, false);
    }

    @Bean
    public Binding orderPaymentBinding(Queue orderPaymentEventsQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentEventsQueue).to(paymentExchange).with("payment.#");
    }
}

package com.ballcom.customer.infrastructure.messaging;



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
    public TopicExchange customerExchange() {
        return new TopicExchange("customer.exchange", true, false);
    }

    @Bean
    public Queue customerQueue() {
        return new Queue("customer-readmodel-queue", true);//durable true = overleefd herstart van rabbitmq
    }

    @Bean
    public Binding customerBinding(Queue customerQueue, TopicExchange customerExchange) { //TODO wat is dit wat doet het
        return BindingBuilder.bind(customerQueue).to(customerExchange).with("customer.#");
    }
}

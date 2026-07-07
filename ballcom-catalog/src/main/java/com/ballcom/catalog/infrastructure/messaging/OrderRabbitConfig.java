package com.ballcom.catalog.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderRabbitConfig {
    @Bean
    public TopicExchange catalogExchange() {
        return new TopicExchange("catalog.exchange", true, false);
    }

    @Bean
    public Queue catalogQueue() {
        return new Queue("catalog-readmodel-queue", true);
    }

    @Bean
    public Binding catalogBinding(Queue catalogQueue, TopicExchange catalogExchange) {
        return BindingBuilder.bind(catalogQueue).to(catalogExchange).with("catalog.#");
    }
}

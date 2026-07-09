package com.ballcom.catalog.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;


@Configuration
public class CatalogRabbitConfig {

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // DIT IS DE ONTBREKENDE SCHAKEL:
        template.setMessageConverter(jsonMessageConverter); 
        
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, 
            MessageConverter jsonMessageConverter) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // DIT IS DE ONTBREKENDE SCHAKEL VOOR DE ONTVANGER:
        factory.setMessageConverter(jsonMessageConverter); 
        
        return factory;
    }
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

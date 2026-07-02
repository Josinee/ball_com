package com.ballcom.shared.messaging;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        // Vertaalt java objecten automatisch naar JSON, is nodig omdat het naar messagebroker(rabbitmq) gaat en niet naar API
        return new Jackson2JsonMessageConverter();
    }
}
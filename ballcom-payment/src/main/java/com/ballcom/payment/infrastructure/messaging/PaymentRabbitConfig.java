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


    //als shipping price berekend is stuurt shipmpent een COSTS_CALCULATED, hier wordt hij opgevangen
    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange("shipment.exchange", true, false);
    }

    @Bean
    public Binding listenToCostsCalculeted(Queue orderPlacedQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(shipmentExchange).with("shipment.costs.calculated");
    }

    @Bean
    public Queue shipmentDeliveryQueue() {
        return new Queue("payment-shipment-delivery-queue", true);
    }

    @Bean
    public Binding shipmentDeliveryBinding(Queue shipmentDeliveryQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(shipmentDeliveryQueue).to(shipmentExchange).with("shipment.order.#");
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
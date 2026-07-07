package com.ballcom.shipment.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShipmentRabbitConfig {

    //shipping is eigenaar van zending
    @Bean
    public TopicExchange shippingExchange(){
        return new TopicExchange("shipping.exchange", true, false);
    }


    //wat wilt shipping ontvangen
    @Bean
    public Queue orderPlacedQueue() {
        return new Queue("shipping-order-placed-queue", true);
    }
    //nodig voor de binding
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange", true, false);
    }

        @Bean
    public Binding orderPlacedBinding(Queue orderPlacedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(orderExchange).with("order.#");
    }






    @Bean
    public Queue paymentMadeQueue() {
        return new Queue("shipping-payment-made-queue", true); 
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange", true, false);
    }

    @Bean
    public Binding paymentMadeBinding(Queue paymentMadeQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentMadeQueue).to(paymentExchange).with("payment.#");
    }



    @Bean
    public Queue deliveryQueue() {
        return new Queue("shipping-delivery-queue", true);
    }

    @Bean
    public Binding deliveryBinding(Queue deliveryQueue, TopicExchange shippingExchange) {
        return BindingBuilder.bind(deliveryQueue).to(shippingExchange).with("shipping.package.delivered");
    }

}
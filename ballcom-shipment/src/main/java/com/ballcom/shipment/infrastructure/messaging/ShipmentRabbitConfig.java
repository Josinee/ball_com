package com.ballcom.shipment.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShipmentRabbitConfig {

    //shipment is eigenaar van zending
    @Bean
    public TopicExchange shipmentExchange(){
        return new TopicExchange("shipment.exchange", true, false);
    }


    //wat wilt shipment ontvangen
    @Bean
    public Queue orderPlacedQueue() {
        return new Queue("shipment-order-placed-queue", true);
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
        return new Queue("shipment-payment-made-queue", true); 
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
        return new Queue("shipment-delivery-queue", true);
    }

    @Bean
    public Binding deliveryBinding(Queue deliveryQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(deliveryQueue).to(shipmentExchange).with("shipment.order.#");
    }

}
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
    public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
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


    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange("shipment.exchange", true, false);
    }

    @Bean
    public Binding orderShipmentBinding(Queue orderQueue, TopicExchange shipmentExchange) {
        return BindingBuilder.bind(orderQueue).to(shipmentExchange).with("shipment.#");
    }

    @Bean
    public Queue orderingCatalogUpdatesQueue() {
        return new Queue("ordering-catalog-updates-queue", true);
    }

    @Bean
    public TopicExchange catalogExchange() {
        return new TopicExchange("catalog.exchange", true, false);
    }

    @Bean
    public Binding orderingCatalogBinding(
        Queue orderingCatalogUpdatesQueue,
        TopicExchange catalogExchange
    ) {
        return BindingBuilder
            .bind(orderingCatalogUpdatesQueue)
            .to(catalogExchange)
            .with("catalog.#");
    }

    @Bean
    public Queue orderingCustomerUpdatesQueue() {
        return new Queue("ordering-customer-updates-queue", true);
    }

    @Bean
    public TopicExchange customerExchange() {
        return new TopicExchange("customer.exchange", true, false);
        }

    @Bean
    public Binding orderingCustomerBinding(
        Queue orderingCustomerUpdatesQueue,
        TopicExchange customerExchange
    ) {
        return BindingBuilder
            .bind(orderingCustomerUpdatesQueue)
            .to(customerExchange)
            .with("customer.#");
    }

}

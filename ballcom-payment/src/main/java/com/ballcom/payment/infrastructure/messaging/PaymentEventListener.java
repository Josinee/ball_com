package com.ballcom.payment.infrastructure.messaging;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.ballcom.payment.application.PaymentCommandHandler;
import com.ballcom.payment.application.ProcessPaymentCommand;
import com.ballcom.payment.domain.PaymentAggregate;
import com.ballcom.payment.domain.PaymentMethod;
import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;

@Component
public class PaymentEventListener {
    private final JdbcTemplate jdbcTemplate;
    private final PaymentCommandHandler commandHandler;

    public PaymentEventListener(JdbcTemplate jdbcTemplate, PaymentCommandHandler commandHandler){
        this.jdbcTemplate = jdbcTemplate;
        this.commandHandler = commandHandler;
    }

    @RabbitListener(queues = "payment-order-placed-queue")
    public void consume(GenericDomainEvent event) {
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if(rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + "al eerder verwerkt");
                return;
            }
            if(EventType.ORDER_PLACED.equals(event.eventType())) {
                try{
                    Map<String, Object> payload = (Map<String, Object>) event.payload();

                    String methodFromEvent = (String) payload.get("paymentMethod");
                    PaymentMethod mappedMethod = PaymentMethod.valueOf(methodFromEvent.toUpperCase());

                    var command = new ProcessPaymentCommand(
                        UUID.fromString((String) payload.get("customerId")),
                        event.aggregateId(), // De orderId
                        new BigDecimal(payload.get("totalAmount").toString()),
                        mappedMethod // Nu volledig typsafe als Enum!
                    );

                    // Stuur door naar de PaymentCommandHandler
                    commandHandler.handle(command);
                    

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}

package com.ballcom.payment.application;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.payment.application.commands.CompletePaymentCommand;
import com.ballcom.payment.application.commands.FailPaymentCommand;
import com.ballcom.payment.application.commands.ProcessPaymentCommand;
import com.ballcom.payment.application.commands.RegisterDeliveryCommand;
import com.ballcom.payment.domain.PaymentAggregate;
import com.ballcom.payment.domain.PaymentMethod;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentCommandHandler {
    private final EventStore eventStore;
    private JdbcTemplate jdbcTemplate;

    public PaymentCommandHandler(EventStore eventStore, JdbcTemplate jdbcTemplate) {
        this.eventStore = eventStore;
        this.jdbcTemplate = jdbcTemplate;
    }


    @Transactional
    public void handle(ProcessPaymentCommand command) {
        if (command.total().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Het totale bedrag moet groter zijn dan 0");
        }

        PaymentAggregate payment = PaymentAggregate.initiate(
            command.paymentId(),
            command.orderId(), 
            command.customerId(), 
            command.total(), 
            command.paymentMethod()
        );
        
        if (command.paymentMethod().equals(PaymentMethod.AFTERPAY)) {
            payment.holdForDelivery(); 
        } 

        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }

    
    @Transactional
    public UUID handle(CompletePaymentCommand command) {
        String lookupSql = "SELECT payment_id FROM order_payment_mapping WHERE order_id = ?";
        UUID paymentId;
        try {
            paymentId = jdbcTemplate.queryForObject(lookupSql, UUID.class, command.orderId());
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new RuntimeException("Kan betaling niet afronden: Geen actieve betaling gevonden voor orderId: " + command.orderId());
        }
        
        List<GenericDomainEvent> history = eventStore.loadEvents(paymentId); 
        
        PaymentAggregate payment = new PaymentAggregate();
        payment.loadFromHistory(history);

        payment.complete();

        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
        return payment.getId();
    }

    
    @Transactional
    public void handle(FailPaymentCommand command) {
        String lookupSql = "SELECT payment_id FROM order_payment_mapping WHERE order_id = ?";
        UUID paymentId;
        try {
            paymentId = jdbcTemplate.queryForObject(lookupSql, UUID.class, command.orderId());
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Kan betaling niet afronden: Geen actieve betaling gevonden voor orderId: " + command.orderId());
        }

        List<GenericDomainEvent> history = eventStore.loadEvents(paymentId);
        PaymentAggregate payment = new PaymentAggregate();
        
        payment.loadFromHistory(history);

        payment.fail(command.reason());

        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }

    @Transactional
    public void handle(RegisterDeliveryCommand command) {
        String lookupSql = "SELECT payment_id FROM order_payment_mapping WHERE order_id = ?";
        UUID paymentId = jdbcTemplate.queryForObject(lookupSql, UUID.class, command.orderId());
        
        List<GenericDomainEvent> history = eventStore.loadEvents(paymentId); 
        PaymentAggregate payment = new PaymentAggregate();
        payment.loadFromHistory(history);

        payment.registerDelivery();

        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }
}
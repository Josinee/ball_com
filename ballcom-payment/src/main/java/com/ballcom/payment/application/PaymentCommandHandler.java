package com.ballcom.payment.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ballcom.payment.domain.*;
import com.ballcom.shared.eventsourcing.EventStore;


@Component
public class PaymentCommandHandler {
    private final EventStore eventStore;

    public PaymentCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public void handle(ProcessPaymentCommand command) {
        PaymentAggregate payment = PaymentAggregate.initiate(command.customerId(), command.orderId(), command.total(), command.paymentMethod());
        
        if(command.paymentMethod().equals(PaymentMethod.PREPAY)) {
            payment.complete();
        } else if (command.paymentMethod().equals(PaymentMethod.AFTERPAY)) {
            payment.holdForDelivery();
        }
        //business rules
        //hoe kan een payment falen

        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getSequenceNumber());
        payment.clearUncommitedEvents();
    }
}

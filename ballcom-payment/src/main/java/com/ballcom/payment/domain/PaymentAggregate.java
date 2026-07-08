package com.ballcom.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;

public class PaymentAggregate extends AggregateRoot {
    private UUID customerId; 
    private UUID orderId;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;

    public PaymentAggregate() {}

    public static PaymentAggregate initiate(UUID paymentId, UUID orderId, UUID customerId, BigDecimal total, PaymentMethod paymentMethod) {
        PaymentAggregate payment = new PaymentAggregate();
        payment.id = paymentId;

        Map<String, Object> payload = Map.of(
            "orderId", orderId.toString(),
            "customerId", customerId.toString(),
            "total", total.toString(),
            "paymentMethod", paymentMethod.name(),
            "status", "INITIATED"
        );

        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),                        
            paymentId,
            payment.getSequenceNumber() + 1,
            EventType.PAYMENT_INITIATED,
            Instant.now(),
            payload
        );
        payment.raiseEvent(event);
        return payment;
    }

    public void registerDelivery() {
        if(this.status != PaymentStatus.AWAITING_DELIVERY) {
            return;
        }
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(), this.getId(), this.getSequenceNumber() + 1, EventType.PAYMENT_DELIVERY_CONFIIRMED, Instant.now(), Map.of("orderId", this.orderId)
        );
        this.raiseEvent(event);
    }




    public void holdForDelivery() {
        if(this.status == PaymentStatus.AWAITING_DELIVERY) return;
        this.emitHoldForDelivery();
    }

    public void complete() {
        if(this.status == PaymentStatus.COMPLETED) return;
        if(this.status == PaymentStatus.AWAITING_DELIVERY) {
            throw new IllegalStateException("AfterPay betaling kan pas worden afgerond nadat het pakket succesvol is BEZORGD (DELIVERED).");
        }
        this.emitComplete();
    }

    public void fail(String reason) {
        if(this.status == PaymentStatus.FAILED) return;
        this.emitFail(reason);
    }




    public void emitHoldForDelivery() {
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),
            this.getId(),
            this.getSequenceNumber() + 1,
            EventType.PAYMENT_AWAITING_DELIVERY,
            Instant.now(),
            Map.of(
                "status", "AWAITING_DELIVERY",
                "orderId", this.orderId.toString(),
                "paymentMethod", this.paymentMethod.name()
            )
        );
        this.raiseEvent(event);
    }

    public void emitComplete() {
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),
            this.getId(),
            this.getSequenceNumber() + 1,
            EventType.PAYMENT_COMPLETED,
            Instant.now(),
            Map.of(
                "status", "COMPLETED",
                "orderId", this.orderId.toString(),
                "paymentMethod", this.paymentMethod.name()
            )
        );
        this.raiseEvent(event);
    }

    public void emitFail(String reason) {
        GenericDomainEvent event = new GenericDomainEvent(
            UUID.randomUUID(),
            this.getId(),
            this.getSequenceNumber() + 1,
            EventType.PAYMENT_FAILED,
            Instant.now(),
            Map.of("status", "FAILED", "orderId", this.orderId.toString(), "reason", reason)
        );

        this.raiseEvent(event);
    }


    @Override
    public void apply(GenericDomainEvent event) {
        this.id = event.aggregateId();
        
        Map<String, Object> payload = event.payload();

        if (EventType.PAYMENT_INITIATED.equals(event.eventType())) {
            this.customerId = UUID.fromString((String) payload.get("customerId"));
            this.orderId = UUID.fromString((String) payload.get("orderId"));
            this.total = new BigDecimal((String) payload.get("total"));
            this.paymentMethod = PaymentMethod.valueOf((String) payload.get("paymentMethod"));
            this.status = PaymentStatus.INITIATED;
        } 
        else if (EventType.PAYMENT_AWAITING_DELIVERY.equals(event.eventType())) {
            this.status = PaymentStatus.AWAITING_DELIVERY;
        } 
        else if (EventType.PAYMENT_COMPLETED.equals(event.eventType())) {
            this.status = PaymentStatus.COMPLETED;
        } 
        else if (EventType.PAYMENT_FAILED.equals(event.eventType())) {
            this.status = PaymentStatus.FAILED;
        } else if (EventType.PAYMENT_DELIVERY_CONFIIRMED.equals(event.eventType())) {
            this.status = PaymentStatus.DELIVERED;
        }

    }

    public PaymentStatus getStatus() { return status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }


}
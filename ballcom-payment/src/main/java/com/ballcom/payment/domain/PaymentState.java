package com.ballcom.payment.domain;

public interface PaymentState {
    default void holdForDelivery(PaymentAggregate aggregate) {
        throw new IllegalStateException("Not allowed to move to state HOLD_FOR_DELIVERY when in state " + this);
    }default void complete(PaymentAggregate aggregate) {
        throw new IllegalStateException("Not allowed to move to state COMPLETE when in state " + this);
    }
    default void fail(PaymentAggregate aggregate, String reason) {
        throw new IllegalStateException("Not allowed to move to state FAIL when in state " + this);
    }
    default void retry(PaymentAggregate aggregate, PaymentMethod newMethod) {
        throw new IllegalStateException("Not allowed to move to state RETRY when in state " + this);
    }
}

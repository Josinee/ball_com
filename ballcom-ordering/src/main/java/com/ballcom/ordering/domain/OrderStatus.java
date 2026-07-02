package com.ballcom.ordering.domain;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED,
    READY_FOR_SHIPMENT,
    SHIPPED,
    DELIVERED
}

package com.ballcom.ordering.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(UUID customerId, List<OrderItemData> items, String paymentMethod) {
   
    public record OrderItemData(UUID productId, int quantity, BigDecimal unitPrice) {}
}
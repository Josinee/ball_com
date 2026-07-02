package com.ballcom.ordering.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderItemRequest(UUID productId, int quantity, BigDecimal unitPrice) {
}

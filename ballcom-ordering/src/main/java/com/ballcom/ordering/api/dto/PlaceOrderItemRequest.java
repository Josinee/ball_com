package com.ballcom.ordering.api.dto;

import java.math.BigDecimal;
import java.util.UUID;
//bij de POST request van een Order wordt deze meegegeven als item
public record PlaceOrderItemRequest(UUID productId, int quantity, BigDecimal unitPrice) {
}

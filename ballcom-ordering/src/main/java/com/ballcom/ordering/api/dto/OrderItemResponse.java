package com.ballcom.ordering.api.dto;

import java.math.BigDecimal;
import java.util.UUID;
//TODO nog niet gebruikt, geeft een specifiek item uit een order
public record OrderItemResponse(UUID productId, int quantity, BigDecimal unitPrice) {
}

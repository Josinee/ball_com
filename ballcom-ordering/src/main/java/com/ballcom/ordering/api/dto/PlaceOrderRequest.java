package com.ballcom.ordering.api.dto;

import java.util.List;
import java.util.UUID;
//order plaatsen dto bij een POST request
public record PlaceOrderRequest(UUID customerId, List<PlaceOrderItemRequest> items, String paymentMethod) {}
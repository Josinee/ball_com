package com.ballcom.ordering.api.dto;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(UUID customerId, List<PlaceOrderItemRequest> items) {}
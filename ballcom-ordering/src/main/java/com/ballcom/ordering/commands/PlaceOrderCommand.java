package com.ballcom.ordering.commands;

import com.ballcom.ordering.domain.OrderItem;


import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(UUID customerId, List<OrderItem> items) {
}
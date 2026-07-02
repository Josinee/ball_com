package com.ballcom.ordering.api;

import com.ballcom.ordering.api.dto.OrderAcceptedResponse;
import com.ballcom.ordering.api.dto.PlaceOrderRequest;
import com.ballcom.ordering.application.OrderCommandHandler;
import com.ballcom.ordering.application.PlaceOrderCommand;
import com.ballcom.ordering.domain.OrderItem;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

//command, doet alleen maar schrijven
//ontvangt HTTP POST request (dto/PlaceOrderRequest), valideert syntax
//bouwt PlaceOrderCommand en geefft deze door aan OrderCommandHandler
// geeft een HTTP 202 accepted met het nieuwe orderId. Doet niks met de database
@RestController
@RequestMapping("/api/orders")
public class OrderCommandController {

    private final OrderCommandHandler commandHandler;
    
    public OrderCommandController(OrderCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }


    @PostMapping
    public ResponseEntity<OrderAcceptedResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList();

        UUID orderId = commandHandler.handle(new PlaceOrderCommand(request.customerId(), items));

        return ResponseEntity.accepted()
                .location(URI.create("/orders/" + orderId))
                .body(new OrderAcceptedResponse(orderId));
    }
}
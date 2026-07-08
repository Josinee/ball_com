package com.ballcom.ordering.api;

import com.ballcom.ordering.api.dto.OrderAcceptedResponse;
import com.ballcom.ordering.api.dto.PlaceOrderRequest;
import com.ballcom.ordering.application.OrderCommandHandler;
import com.ballcom.ordering.application.PlaceOrderCommand;
import com.ballcom.shared.ErrorResponse;

import org.springframework.http.HttpStatus;
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


    @PostMapping("/placeorder")
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request) {
        try {
            if (request == null || request.items() == null || request.items().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(422, "Order has to consist of at least 1 product"));
            }
            List<PlaceOrderCommand.OrderItemData> commandItems = request.items().stream()
                    .map(i -> new PlaceOrderCommand.OrderItemData(i.productId(), i.quantity(), i.unitPrice())).toList();

            UUID orderId = commandHandler.handle(new PlaceOrderCommand(request.customerId(), commandItems, request.paymentMethod()));

            return ResponseEntity.accepted().location(URI.create("/orders/" + orderId)).body(new OrderAcceptedResponse(orderId));

        } catch (IllegalStateException | IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getLocalizedMessage()));
        }
    }
}
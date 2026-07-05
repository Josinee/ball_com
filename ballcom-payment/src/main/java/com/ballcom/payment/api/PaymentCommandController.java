package com.ballcom.payment.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.payment.application.CompletePaymentCommand;
import com.ballcom.payment.application.PaymentCommandHandler;



@RestController
@RequestMapping("/payments")
public class PaymentCommandController{
    private final PaymentCommandHandler commandHandler;

    public PaymentCommandController(PaymentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping("pay/{orderId}")
    public ResponseEntity<String> mockPayment(@PathVariable UUID orderId) {
        if(orderId == null) {
            throw new IllegalArgumentException("No order with orderId " + orderId + " found");
        }
        var command = new CompletePaymentCommand(orderId);

        commandHandler.handle(command);
        return ResponseEntity.accepted().body("Payment made");
    }
}
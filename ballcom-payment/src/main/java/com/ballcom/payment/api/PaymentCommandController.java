package com.ballcom.payment.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.payment.application.CompletePaymentCommand;
import com.ballcom.payment.application.FailPaymentCommand;
import com.ballcom.payment.application.PaymentCommandHandler;
import com.ballcom.shared.ErrorResponse;




@RestController
@RequestMapping("/payments")
public class PaymentCommandController{
    private final PaymentCommandHandler commandHandler;
    private JdbcTemplate jdbcTemplate;

    public PaymentCommandController(PaymentCommandHandler commandHandler, JdbcTemplate jdbcTemplate) {
        this.commandHandler = commandHandler;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("pay/{orderId}")
    public ResponseEntity<?> mockPayment(@PathVariable UUID orderId) {
        try {
            if (orderId == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse(422, "No order with orderId " + orderId + " found"));
            }
            
            var command = new CompletePaymentCommand(orderId);

            commandHandler.handle(command);
            return ResponseEntity.accepted().body("Payment made");
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }

    //ik kan niet bedenken hoe een betaling zou falen in deze context dus het moet handmatig
    @PostMapping("pay/{orderId}/fail")
    public ResponseEntity<?> mockPaymentFailure(@PathVariable UUID orderId, @RequestParam String reason) {
        try {
            if (orderId == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(422, "No order with orderId found because it is null"));
            }
            UUID paymentId = jdbcTemplate.queryForObject(
                "SELECT payment_id FROM order_payment_mapping WHERE order_id = ?", 
                UUID.class, 
                orderId
            );
                
            var command = new FailPaymentCommand(paymentId, reason);
            commandHandler.handle(command);
                
            return ResponseEntity.accepted().body("Payment failed simulated with reason: " + reason);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getLocalizedMessage()));
        }
        
    }
}
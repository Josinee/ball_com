package com.ballcom.customer.api;


import com.ballcom.customer.api.dto.CustomerRegisterRequest;
import com.ballcom.customer.api.dto.CustomerRegisteredResponse;
import com.ballcom.customer.application.CustomerCommandHandler;
import com.ballcom.customer.application.commands.RegisterCustomerCommand;
import com.ballcom.shared.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerCommandController {
    private final CustomerCommandHandler commandHandler;

    public CustomerCommandController(CustomerCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping
    public ResponseEntity<?> registerCustomer(@RequestBody CustomerRegisterRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(422, "Registration needs to contain companyname, name, phone number and address"));
            }

            var command = new RegisterCustomerCommand(
                    request.companyName(),
                    request.firstName(),
                    request.lastName(),
                    request.phoneNumber(),
                    request.street(),
                    request.houseNumber(),
                    request.city(),
                    request.zipCode()
            );
            
            UUID customerId = commandHandler.handle(command);
            return ResponseEntity.accepted().body(new CustomerRegisteredResponse(customerId));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getLocalizedMessage()));
        }
    }
}
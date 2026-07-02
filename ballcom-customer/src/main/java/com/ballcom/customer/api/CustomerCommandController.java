package com.ballcom.customer.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.customer.api.dto.CustomerRegisterRequest;
import com.ballcom.customer.api.dto.CustomerRegisteredResponse;
import com.ballcom.customer.application.CustomerCommandHandler;
import com.ballcom.customer.application.RegisterCustomerCommand;

@RestController
@RequestMapping("/api/customers")
public class CustomerCommandController {
    private final CustomerCommandHandler commandHandler;

    public CustomerCommandController(CustomerCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping
    public ResponseEntity<CustomerRegisteredResponse> registerCustomer(@RequestBody CustomerRegisterRequest request) {

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
    }
}
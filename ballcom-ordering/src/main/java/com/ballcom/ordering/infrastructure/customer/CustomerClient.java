package com.ballcom.ordering.infrastructure.customer;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CustomerClient {
    private final RestClient restClient;

    public CustomerClient(@Value("${services.customer.url}") String customerServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(customerServiceUrl)
                .build();
    }

    public boolean customerExists(UUID customerId) {
        try {
            restClient.get()
                    .uri("/api/customers/{customerId}", customerId)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }
            throw e;
        }
    }
}
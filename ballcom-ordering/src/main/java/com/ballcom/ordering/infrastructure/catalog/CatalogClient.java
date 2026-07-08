package com.ballcom.ordering.infrastructure.catalog;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CatalogClient {
    private final RestClient restClient;

    public CatalogClient(@Value("${services.catalog.url}") String catalogServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(catalogServiceUrl)
                .build();
    }

    public CatalogItemResponse getCatalogItem(UUID productId) {
        try {
            return restClient.get()
                    .uri("/api/catalog/{productId}", productId)
                    .retrieve()
                    .body(CatalogItemResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("Product does not exist in catalog: " + productId);
            }
            throw e;
        }
    }
}
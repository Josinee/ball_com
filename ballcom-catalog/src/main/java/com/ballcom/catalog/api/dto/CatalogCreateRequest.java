package com.ballcom.catalog.api.dto;

public record CatalogCreateRequest (
    String itemName,
    String price,
    String description,
    String category,
    String availability
) {}

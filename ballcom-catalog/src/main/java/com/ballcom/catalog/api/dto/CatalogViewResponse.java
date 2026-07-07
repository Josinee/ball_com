package com.ballcom.catalog.api.dto;

import java.util.UUID;

public record CatalogViewResponse (
    UUID catalogId,
    String itemName,
    String price,
    String description,
    String category,
    String availability
)
{}

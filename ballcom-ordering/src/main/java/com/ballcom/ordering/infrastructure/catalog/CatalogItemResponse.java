package com.ballcom.ordering.infrastructure.catalog;

import java.util.UUID;

public record CatalogItemResponse (
     UUID catalogId,
    String itemName,
    String price,
    String description,
    String category,
    String availability,
    String owner
) {}

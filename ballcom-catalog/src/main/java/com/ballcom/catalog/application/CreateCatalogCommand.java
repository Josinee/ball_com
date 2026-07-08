package com.ballcom.catalog.application;

public record CreateCatalogCommand (
    String itemName,
    String price,
    String description,
    String category,
    String availability,
    String owner
)
{}

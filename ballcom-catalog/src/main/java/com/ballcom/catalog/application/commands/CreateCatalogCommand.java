package com.ballcom.catalog.application.commands;

public record CreateCatalogCommand (
    String itemName,
    String price,
    String description,
    String category,
    String availability
)
{}

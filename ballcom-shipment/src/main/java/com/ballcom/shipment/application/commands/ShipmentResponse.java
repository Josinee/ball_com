package com.ballcom.shipment.application.commands;

import java.util.UUID;

public record ShipmentResponse(UUID shipmentId, String status) {}
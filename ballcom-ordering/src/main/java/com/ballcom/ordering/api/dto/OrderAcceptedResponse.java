package com.ballcom.ordering.api.dto;

import java.util.UUID;
// geeft terug dat het order aanmaken goed is gegaan met een orderId
public record OrderAcceptedResponse(UUID orderId) {
}

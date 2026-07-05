package com.ballcom.ordering.application;

import java.util.UUID;

public record ConfirmOrderPaymentCommand(UUID orderId) {}
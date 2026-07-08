package com.ballcom.ordering.application.commands;

import java.util.UUID;

public record ConfirmOrderPaymentCommand(UUID orderId) {}
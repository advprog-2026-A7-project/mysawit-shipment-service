package com.mysawit.shipment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusRequest(
        @NotBlank(message = "Invalid status value")
        String status
) {
}

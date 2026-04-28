package com.mysawit.shipment.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminApprovalRequest(
        @NotBlank(message = "Admin approval status is required")
        String status
) {
}

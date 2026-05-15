package com.mysawit.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AdminApprovalRequest(
        @NotBlank(message = "Admin approval status is required")
        String status,
        String rejectionReason,
        @Positive(message = "Accepted kg must be positive")
        Double kgAccepted
) {
    public AdminApprovalRequest(String status) {
        this(status, null, null);
    }
}

package com.mysawit.shipment.dto;

import jakarta.validation.constraints.NotBlank;

public record MandorApprovalRequest(
        @NotBlank(message = "Mandor approval status is required")
        String status,
        String rejectionReason
) {
}

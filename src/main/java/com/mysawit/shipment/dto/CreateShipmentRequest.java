package com.mysawit.shipment.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateShipmentRequest(
        @NotNull(message = "Supir user ID is required")
        UUID supirUserId,

        @NotBlank(message = "Destination is required")
        String destination,

        @NotEmpty(message = "At least one harvest item is required")
        @Valid
        List<HarvestItem> items
) {
    public record HarvestItem(
            @NotNull(message = "Harvest ID is required")
            UUID harvestId,

            @NotNull(message = "Weight kg is required")
            @Positive(message = "Weight kg must be positive")
            Double weightKg
    ) {
    }
}

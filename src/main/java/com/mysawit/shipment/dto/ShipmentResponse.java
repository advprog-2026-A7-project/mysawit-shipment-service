package com.mysawit.shipment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;

public record ShipmentResponse(
        UUID id,
        UUID harvestId,
        UUID supirUserId,
        String destination,
        Double totalKg,
        ShipmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ShipmentResponse fromEntity(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getHarvestId(),
                shipment.getSupirUserId(),
                shipment.getDestination(),
                shipment.getTotalKg(),
                shipment.getStatus(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}

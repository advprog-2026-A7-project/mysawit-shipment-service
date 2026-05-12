package com.mysawit.shipment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;

public record ShipmentResponse(
        UUID id,
        UUID mandorUserId,
        UUID supirUserId,
        String destination,
        Double totalKg,
        ShipmentStatus status,
        List<ShipmentItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ShipmentResponse fromEntity(Shipment shipment) {
        List<ShipmentItemResponse> itemResponses = shipment.getItems().stream()
                .map(ShipmentItemResponse::fromEntity)
                .toList();
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getMandorUserId(),
                shipment.getSupirUserId(),
                shipment.getDestination(),
                shipment.getTotalKg(),
                shipment.getStatus(),
                itemResponses,
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}

package com.mysawit.shipment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;

public record ShipmentResponse(
        UUID id,
        UUID mandorUserId,
        String mandorName,
        UUID supirUserId,
        String supirName,
        String destination,
        String plantationId,
        Double totalKg,
        Double kgAccepted,
        String rejectionReason,
        ShipmentStatus status,
        List<ShipmentItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime mandorReviewedAt,
        OffsetDateTime adminReviewedAt
) {
    public static ShipmentResponse fromEntity(Shipment shipment) {
        List<ShipmentItemResponse> itemResponses = shipment.getItems().stream()
                .map(ShipmentItemResponse::fromEntity)
                .toList();
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getMandorUserId(),
                shipment.getMandorName(),
                shipment.getSupirUserId(),
                shipment.getSupirName(),
                shipment.getDestination(),
                shipment.getPlantationId(),
                shipment.getTotalKg(),
                shipment.getKgAccepted(),
                shipment.getRejectionReason(),
                shipment.getStatus(),
                itemResponses,
                shipment.getCreatedAt(),
                shipment.getUpdatedAt(),
                shipment.getMandorReviewedAt(),
                shipment.getAdminReviewedAt()
        );
    }
}

package com.mysawit.shipment.dto;

import java.util.UUID;

import com.mysawit.shipment.model.WorkerPlantationAssignment;

public record SupirAssignmentResponse(
        UUID userId,
        String name,
        String plantationId
) {
    public static SupirAssignmentResponse fromEntity(WorkerPlantationAssignment assignment) {
        return new SupirAssignmentResponse(
                assignment.getUserId(),
                assignment.getName(),
                assignment.getPlantationId()
        );
    }
}

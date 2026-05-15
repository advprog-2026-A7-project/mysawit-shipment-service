package com.mysawit.shipment.dto;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.mysawit.shipment.model.WorkerPlantationAssignment;

class SupirAssignmentResponseTest {

    @Test
    void fromEntityMapsAssignmentFields() {
        UUID userId = UUID.fromString("42424242-4242-4242-4242-424242424242");
        WorkerPlantationAssignment assignment = new WorkerPlantationAssignment();
        assignment.setUserId(userId);
        assignment.setName("Supir One");
        assignment.setPlantationId("plantation-1");

        SupirAssignmentResponse response = SupirAssignmentResponse.fromEntity(assignment);

        assertEquals(userId, response.userId());
        assertEquals("Supir One", response.name());
        assertEquals("plantation-1", response.plantationId());
    }
}

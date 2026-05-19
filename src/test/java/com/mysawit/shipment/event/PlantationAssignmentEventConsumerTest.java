package com.mysawit.shipment.event;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.mysawit.shipment.service.PlantationAssignmentReplicaService;

class PlantationAssignmentEventConsumerTest {

    @Test
    void onPlantationAssignmentDelegatesToReplicaService() {
        PlantationAssignmentReplicaService replicaService = mock(PlantationAssignmentReplicaService.class);
        PlantationAssignmentEventConsumer consumer = new PlantationAssignmentEventConsumer(replicaService);
        PlantationAssignmentEvent event = new PlantationAssignmentEvent(
                "event-1",
                "11111111-1111-1111-1111-111111111111",
                "Supir Demo",
                "SUPIR",
                "00000000-0000-0000-0000-000000000001",
                PlantationAssignmentEvent.AssignmentAction.ASSIGNED,
                OffsetDateTime.now()
        );

        consumer.onPlantationAssignment(event);

        verify(replicaService).applyAssignment(event);
    }
}

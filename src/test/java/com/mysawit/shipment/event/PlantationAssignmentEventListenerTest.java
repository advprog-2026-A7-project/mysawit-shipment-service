package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;
import com.mysawit.shipment.service.WorkerAssignmentLookupService;

class PlantationAssignmentEventListenerTest {

    private static final UUID USER_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-04-14T10:00:00Z");
    private static final String PLANTATION_ID = "plantation-1";
    private static final String WORKER_NAME = "Name";
    private static final String ROLE_SUPIR = "SUPIR";

    private WorkerPlantationAssignmentRepository workerPlantationAssignmentRepository;
    private WorkerAssignmentLookupService workerAssignmentLookup;
    private PlantationAssignmentEventListener listener;

    @BeforeEach
    void setUp() {
        workerPlantationAssignmentRepository = mock(WorkerPlantationAssignmentRepository.class);
        workerAssignmentLookup = mock(WorkerAssignmentLookupService.class);
        listener = new PlantationAssignmentEventListener(
                workerPlantationAssignmentRepository,
                workerAssignmentLookup
        );
    }

    @Test
    void handleAssignmentEventSavesNormalizedAssignment() {
        PlantationAssignmentEvent event = new PlantationAssignmentEvent(
                "event-1",
                USER_ID,
                " Supir One ",
                "supir",
                " " + PLANTATION_ID + " ",
                "ASSIGNED",
                OCCURRED_AT
        );
        when(workerPlantationAssignmentRepository.findById(USER_ID)).thenReturn(Optional.empty());

        listener.handleAssignmentEvent(event);

        ArgumentCaptor<WorkerPlantationAssignment> captor = ArgumentCaptor.forClass(WorkerPlantationAssignment.class);
        verify(workerPlantationAssignmentRepository).save(captor.capture());
        WorkerPlantationAssignment assignment = captor.getValue();
        assertEquals(USER_ID, assignment.getUserId());
        assertEquals(ROLE_SUPIR, assignment.getRole());
        assertEquals("Supir One", assignment.getName());
        assertEquals(PLANTATION_ID, assignment.getPlantationId());
        assertEquals("event-1", assignment.getLastEventId());
        assertEquals(OCCURRED_AT, assignment.getUpdatedAt());
    }

    @Test
    void handleAssignmentEventUpdatesExistingAndDefaultsTimestamp() {
        WorkerPlantationAssignment existing = new WorkerPlantationAssignment();
        when(workerPlantationAssignmentRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        listener.handleAssignmentEvent(new PlantationAssignmentEvent(
                "event-2",
                USER_ID,
                " ",
                "MANDOR",
                PLANTATION_ID,
                null,
                null
        ));

        verify(workerPlantationAssignmentRepository).save(existing);
        assertEquals(USER_ID, existing.getUserId());
        assertEquals("MANDOR", existing.getRole());
        assertNull(existing.getName());
    }

    @Test
    void handleAssignmentEventSavesNullName() {
        PlantationAssignmentEvent event = new PlantationAssignmentEvent(
                "event-2",
                USER_ID,
                null,
                ROLE_SUPIR,
                PLANTATION_ID,
                null,
                OCCURRED_AT
        );
        when(workerPlantationAssignmentRepository.findById(USER_ID)).thenReturn(Optional.empty());

        listener.handleAssignmentEvent(event);

        ArgumentCaptor<WorkerPlantationAssignment> captor = ArgumentCaptor.forClass(WorkerPlantationAssignment.class);
        verify(workerPlantationAssignmentRepository).save(captor.capture());
        assertNull(captor.getValue().getName());
    }

    @Test
    void handleAssignmentEventDeletesOnUnassigned() {
        listener.handleAssignmentEvent(new PlantationAssignmentEvent(
                "event-3",
                USER_ID,
                "Supir One",
                ROLE_SUPIR,
                null,
                "UNASSIGNED",
                OCCURRED_AT
        ));

        verify(workerPlantationAssignmentRepository).deleteById(USER_ID);
    }

    @Test
    void handleAssignmentEventIgnoresInvalidPayloads() {
        listener.handleAssignmentEvent(null);
        listener.handleAssignmentEvent(new PlantationAssignmentEvent("e", null, WORKER_NAME, ROLE_SUPIR, PLANTATION_ID, null, null));
        listener.handleAssignmentEvent(new PlantationAssignmentEvent("e", USER_ID, WORKER_NAME, null, PLANTATION_ID, null, null));
        listener.handleAssignmentEvent(new PlantationAssignmentEvent("e", USER_ID, WORKER_NAME, " ", PLANTATION_ID, null, null));
        listener.handleAssignmentEvent(new PlantationAssignmentEvent("e", USER_ID, WORKER_NAME, ROLE_SUPIR, null, null, null));
        listener.handleAssignmentEvent(new PlantationAssignmentEvent("e", USER_ID, WORKER_NAME, ROLE_SUPIR, " ", null, null));

        verify(workerPlantationAssignmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

package com.mysawit.shipment.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mysawit.shipment.event.PlantationAssignmentEvent;

class PlantationAssignmentReplicaServiceTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String PLANTATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final String ROLE_SUPIR = "SUPIR";
    private static final String SUPIR_NAME = "Supir Demo";

    private JdbcTemplate jdbcTemplate;
    private PlantationAssignmentReplicaService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new PlantationAssignmentReplicaService(jdbcTemplate);
    }

    @Test
    void applyAssignmentStoresPlantationId() {
        PlantationAssignmentEvent event = new PlantationAssignmentEvent(
                "event-1",
                USER_ID,
                SUPIR_NAME,
                ROLE_SUPIR,
                PLANTATION_ID,
                PlantationAssignmentEvent.AssignmentAction.ASSIGNED,
                OffsetDateTime.now()
        );

        service.applyAssignment(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(UUID.fromString(USER_ID)),
                eq(SUPIR_NAME),
                eq(ROLE_SUPIR),
                eq(PLANTATION_ID)
        );
    }

    @Test
    void applyAssignmentClearsPlantationIdWhenUnassigned() {
        PlantationAssignmentEvent event = new PlantationAssignmentEvent(
                "event-1",
                USER_ID,
                SUPIR_NAME,
                ROLE_SUPIR,
                PLANTATION_ID,
                PlantationAssignmentEvent.AssignmentAction.UNASSIGNED,
                OffsetDateTime.now()
        );

        service.applyAssignment(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(UUID.fromString(USER_ID)),
                eq(SUPIR_NAME),
                eq(ROLE_SUPIR),
                isNull()
        );
    }
}

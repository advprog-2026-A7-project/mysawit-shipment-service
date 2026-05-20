package com.mysawit.shipment.event;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class ShipmentInboundEventPayloadTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID HARVEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID HARVESTER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FOREMAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant INSTANT = Instant.parse("2026-05-19T01:00:00Z");
    private static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.parse("2026-05-19T01:00:00Z");
    private static final String PLANTATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final String ROLE_SUPIR = "SUPIR";
    private static final String SUPIR_NAME = "Supir Demo";

    @Test
    void userRegisteredEventSupportsNoArgsAndSetters() {
        UserRegisteredEvent event = new UserRegisteredEvent();

        event.setUserId(USER_ID);
        event.setEmail("supir@mysawit.local");
        event.setRole(ROLE_SUPIR);
        event.setUsername(SUPIR_NAME);

        assertEquals(USER_ID, event.getUserId());
        assertEquals("supir@mysawit.local", event.getEmail());
        assertEquals(ROLE_SUPIR, event.getRole());
        assertEquals(SUPIR_NAME, event.getUsername());
    }

    @Test
    void userAssignmentEventSupportsAllArgsConstructor() {
        UserAssignmentEvent event = new UserAssignmentEvent(
                USER_ID,
                "55555555-5555-5555-5555-555555555555",
                "Mandor Demo",
                UserAssignmentEvent.AssignmentAction.ASSIGNED,
                INSTANT
        );

        assertEquals(USER_ID, event.getUserId());
        assertEquals("55555555-5555-5555-5555-555555555555", event.getMandorId());
        assertEquals("Mandor Demo", event.getMandorName());
        assertEquals(UserAssignmentEvent.AssignmentAction.ASSIGNED, event.getAction());
        assertEquals(INSTANT, event.getOccurredAt());
    }

    @Test
    void userDeletedEventSupportsAllArgsConstructor() {
        UserDeletedEvent event = new UserDeletedEvent(USER_ID, ROLE_SUPIR, null, INSTANT);

        assertEquals(USER_ID, event.getUserId());
        assertEquals(ROLE_SUPIR, event.getRole());
        assertNull(event.getPreviousMandorId());
        assertEquals(INSTANT, event.getOccurredAt());
    }

    @Test
    void harvestEventSupportsAllArgsConstructor() {
        HarvestEvent event = new HarvestEvent(
                "event-1",
                HARVEST_ID,
                HARVESTER_ID,
                FOREMAN_ID,
                PLANTATION_ID,
                120.5,
                "APPROVED",
                OFFSET_DATE_TIME
        );

        assertEquals("event-1", event.getEventId());
        assertEquals(HARVEST_ID, event.getHarvestId());
        assertEquals(HARVESTER_ID, event.getHarvesterId());
        assertEquals(FOREMAN_ID, event.getForemanId());
        assertEquals(PLANTATION_ID, event.getPlantationId());
        assertEquals(120.5, event.getWeight());
        assertEquals("APPROVED", event.getStatus());
        assertEquals(OFFSET_DATE_TIME, event.getOccurredAt());
    }

    @Test
    void plantationAssignmentEventSupportsNoArgsAndSetters() {
        PlantationAssignmentEvent event = new PlantationAssignmentEvent();

        event.setEventId("event-2");
        event.setUserId(USER_ID);
        event.setName(SUPIR_NAME);
        event.setRole(ROLE_SUPIR);
        event.setPlantationId(PLANTATION_ID);
        event.setAction(PlantationAssignmentEvent.AssignmentAction.UNASSIGNED);
        event.setOccurredAt(OFFSET_DATE_TIME);

        assertEquals("event-2", event.getEventId());
        assertEquals(USER_ID, event.getUserId());
        assertEquals(SUPIR_NAME, event.getName());
        assertEquals(ROLE_SUPIR, event.getRole());
        assertEquals(PLANTATION_ID, event.getPlantationId());
        assertEquals(PlantationAssignmentEvent.AssignmentAction.UNASSIGNED, event.getAction());
        assertEquals(OFFSET_DATE_TIME, event.getOccurredAt());
    }
}

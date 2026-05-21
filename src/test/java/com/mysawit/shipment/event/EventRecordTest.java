package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class EventRecordTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-04-14T10:00:00Z");
    private static final String ROLE_SUPIR = "SUPIR";

    @Test
    void plantationAssignmentEventStoresValues() {
        PlantationAssignmentEvent event = new PlantationAssignmentEvent("e", ID, "Name", ROLE_SUPIR, "p", "ASSIGNED", OCCURRED_AT);

        assertEquals("e", event.eventId());
        assertEquals(ID, event.userId());
        assertEquals("Name", event.name());
        assertEquals(ROLE_SUPIR, event.role());
        assertEquals("p", event.plantationId());
        assertEquals("ASSIGNED", event.action());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }

    @Test
    void plantationAssignmentEventSupportsNullRecordStyleValues() {
        PlantationAssignmentEvent event = new PlantationAssignmentEvent("e", null, "Name", ROLE_SUPIR, "p", " ", OCCURRED_AT);

        assertNull(event.userId());
        assertNull(event.action());
    }

    @Test
    void shipmentPayrollEventStoresValues() {
        ShipmentPayrollEvent event = new ShipmentPayrollEvent("e", ID, ID, ROLE_SUPIR, 10.0, List.of(ID), OCCURRED_AT);

        assertEquals("e", event.eventId());
        assertEquals(ID, event.shipmentId());
        assertEquals(ID, event.employeeId());
        assertEquals(ROLE_SUPIR, event.employeeRole());
        assertEquals(10.0, event.kg());
        assertEquals(List.of(ID), event.harvestIds());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }
}

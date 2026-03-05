package com.mysawit.shipment.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentTest {

    @Test
    void gettersSettersAndDefaultsWork() {
        Shipment shipment = new Shipment();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 1, 10, 0);

        assertEquals("MEMUAT", shipment.getStatus());

        shipment.setId(1L);
        shipment.setHarvestId(2L);
        shipment.setSupirUserId(3L);
        shipment.setDestination("Jakarta");
        shipment.setTotalKg(200.0);
        shipment.setStatus("MENGIRIM");
        shipment.setCreatedAt(createdAt);
        shipment.setUpdatedAt(updatedAt);

        assertEquals(1L, shipment.getId());
        assertEquals(2L, shipment.getHarvestId());
        assertEquals(3L, shipment.getSupirUserId());
        assertEquals("Jakarta", shipment.getDestination());
        assertEquals(200.0, shipment.getTotalKg());
        assertEquals("MENGIRIM", shipment.getStatus());
        assertEquals(createdAt, shipment.getCreatedAt());
        assertEquals(updatedAt, shipment.getUpdatedAt());
    }

    @Test
    void lifecycleHooksSetTimestamps() {
        Shipment shipment = new Shipment();

        shipment.onCreate();

        assertNotNull(shipment.getCreatedAt());
        assertNotNull(shipment.getUpdatedAt());

        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        shipment.setUpdatedAt(beforeUpdate.minusDays(1));

        shipment.onUpdate();

        assertTrue(shipment.getUpdatedAt().isAfter(beforeUpdate));
    }
}

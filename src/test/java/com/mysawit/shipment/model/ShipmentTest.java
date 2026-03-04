package com.mysawit.shipment.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentTest {

    @Test
    void gettersSettersAndDefaultsWork() {
        Shipment shipment = new Shipment();
        LocalDateTime shipmentDate = LocalDateTime.of(2026, 3, 1, 8, 0);
        LocalDateTime deliveryDate = LocalDateTime.of(2026, 3, 2, 8, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 1, 10, 0);

        assertEquals("PENDING", shipment.getStatus());

        shipment.setId(1L);
        shipment.setHarvestId(2L);
        shipment.setDestination("Jakarta");
        shipment.setWeight(200.0);
        shipment.setStatus("IN_TRANSIT");
        shipment.setShipperName("name");
        shipment.setVehicleNumber("B1234CD");
        shipment.setShipmentDate(shipmentDate);
        shipment.setDeliveryDate(deliveryDate);
        shipment.setNotes("note");
        shipment.setCreatedAt(createdAt);
        shipment.setUpdatedAt(updatedAt);

        assertEquals(1L, shipment.getId());
        assertEquals(2L, shipment.getHarvestId());
        assertEquals("Jakarta", shipment.getDestination());
        assertEquals(200.0, shipment.getWeight());
        assertEquals("IN_TRANSIT", shipment.getStatus());
        assertEquals("name", shipment.getShipperName());
        assertEquals("B1234CD", shipment.getVehicleNumber());
        assertEquals(shipmentDate, shipment.getShipmentDate());
        assertEquals(deliveryDate, shipment.getDeliveryDate());
        assertEquals("note", shipment.getNotes());
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

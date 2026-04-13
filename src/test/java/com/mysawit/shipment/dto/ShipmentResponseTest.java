package com.mysawit.shipment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;

class ShipmentResponseTest {

    @Test
    void fromEntityMapsAllFields() {
        Shipment shipment = new Shipment();
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID harvestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID supirUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 1, 10, 0);

        shipment.setId(id);
        shipment.setHarvestId(harvestId);
        shipment.setSupirUserId(supirUserId);
        shipment.setDestination("Jakarta");
        shipment.setTotalKg(150.5);
        shipment.setStatus(ShipmentStatus.MENGIRIM);
        shipment.setCreatedAt(createdAt);
        shipment.setUpdatedAt(updatedAt);

        ShipmentResponse response = ShipmentResponse.fromEntity(shipment);

        assertEquals(id, response.id());
        assertEquals(harvestId, response.harvestId());
        assertEquals(supirUserId, response.supirUserId());
        assertEquals("Jakarta", response.destination());
        assertEquals(150.5, response.totalKg());
        assertEquals(ShipmentStatus.MENGIRIM, response.status());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void fromEntityHandlesNullTimestamps() {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setHarvestId(UUID.randomUUID());
        shipment.setSupirUserId(UUID.randomUUID());
        shipment.setDestination("Surabaya");
        shipment.setTotalKg(80.0);

        ShipmentResponse response = ShipmentResponse.fromEntity(shipment);

        assertEquals(ShipmentStatus.MEMUAT, response.status());
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }
}

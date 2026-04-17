package com.mysawit.shipment.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.ShipmentItem;

class ShipmentResponseTest {

    @Test
    void fromEntityMapsAllFields() {
        Shipment shipment = new Shipment();
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID mandorUserId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID supirUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 1, 10, 0);

        ShipmentItem item = new ShipmentItem();
        item.setHarvestId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        item.setWeightKg(150.5);

        shipment.setId(id);
        shipment.setMandorUserId(mandorUserId);
        shipment.setSupirUserId(supirUserId);
        shipment.setDestination("Jakarta");
        shipment.setTotalKg(150.5);
        shipment.setStatus(ShipmentStatus.MENGIRIM);
        shipment.setItems(List.of(item));
        shipment.setCreatedAt(createdAt);
        shipment.setUpdatedAt(updatedAt);

        ShipmentResponse response = ShipmentResponse.fromEntity(shipment);

        assertEquals(id, response.id());
        assertEquals(mandorUserId, response.mandorUserId());
        assertEquals(supirUserId, response.supirUserId());
        assertEquals("Jakarta", response.destination());
        assertEquals(150.5, response.totalKg());
        assertEquals(ShipmentStatus.MENGIRIM, response.status());
        assertEquals(1, response.items().size());
        assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                response.items().get(0).harvestId());
        assertEquals(150.5, response.items().get(0).weightKg());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void fromEntityHandlesNullTimestamps() {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setMandorUserId(UUID.randomUUID());
        shipment.setSupirUserId(UUID.randomUUID());
        shipment.setDestination("Surabaya");
        shipment.setTotalKg(80.0);

        ShipmentResponse response = ShipmentResponse.fromEntity(shipment);

        assertEquals(ShipmentStatus.MEMUAT, response.status());
        assertTrue(response.items().isEmpty());
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }
}

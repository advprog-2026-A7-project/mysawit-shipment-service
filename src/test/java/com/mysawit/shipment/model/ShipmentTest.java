package com.mysawit.shipment.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mysawit.shipment.domain.ShipmentStatus;

class ShipmentTest {

    @Test
    void gettersSettersAndDefaultsWork() {
        Shipment shipment = new Shipment();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 1, 10, 0);
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID mandorUserId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID supirUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        assertEquals(ShipmentStatus.MEMUAT, shipment.getStatus());

        shipment.setId(id);
        shipment.setMandorUserId(mandorUserId);
        shipment.setSupirUserId(supirUserId);
        shipment.setDestination("Jakarta");
        shipment.setTotalKg(200.0);
        shipment.setStatus(ShipmentStatus.MENGIRIM);
        shipment.setCreatedAt(createdAt);
        shipment.setUpdatedAt(updatedAt);

        assertEquals(id, shipment.getId());
        assertEquals(mandorUserId, shipment.getMandorUserId());
        assertEquals(supirUserId, shipment.getSupirUserId());
        assertEquals("Jakarta", shipment.getDestination());
        assertEquals(200.0, shipment.getTotalKg());
        assertEquals(ShipmentStatus.MENGIRIM, shipment.getStatus());
        assertEquals(createdAt, shipment.getCreatedAt());
        assertEquals(updatedAt, shipment.getUpdatedAt());
    }

    @Test
    void itemsCollectionDefaultsToEmptyList() {
        Shipment shipment = new Shipment();

        assertNotNull(shipment.getItems());
        assertTrue(shipment.getItems().isEmpty());
    }

    @Test
    void itemsCollectionCanBeSet() {
        Shipment shipment = new Shipment();
        ShipmentItem item = new ShipmentItem();
        item.setHarvestId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        item.setWeightKg(120.0);

        shipment.setItems(List.of(item));

        assertEquals(1, shipment.getItems().size());
        assertEquals(120.0, shipment.getItems().get(0).getWeightKg());
    }

    @Test
    void addItemStoresItemAndSetsBackReference() {
        Shipment shipment = new Shipment();
        ShipmentItem item = new ShipmentItem();
        item.setHarvestId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        item.setWeightKg(120.0);

        shipment.addItem(item);

        assertEquals(1, shipment.getItems().size());
        assertSame(shipment, item.getShipment());
        assertSame(item, shipment.getItems().get(0));
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

package com.mysawit.shipment.model;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class ShipmentItemTest {

    @Test
    void gettersSettersWork() {
        ShipmentItem item = new ShipmentItem();
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID harvestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Shipment shipment = new Shipment();

        item.setId(id);
        item.setHarvestId(harvestId);
        item.setWeightKg(150.0);
        item.setShipment(shipment);

        assertEquals(id, item.getId());
        assertEquals(harvestId, item.getHarvestId());
        assertEquals(150.0, item.getWeightKg());
        assertSame(shipment, item.getShipment());
    }
}

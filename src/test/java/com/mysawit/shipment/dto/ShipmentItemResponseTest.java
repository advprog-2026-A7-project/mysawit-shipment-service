package com.mysawit.shipment.dto;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.mysawit.shipment.model.ShipmentItem;

class ShipmentItemResponseTest {

    @Test
    void fromEntityMapsAllFields() {
        ShipmentItem item = new ShipmentItem();
        UUID harvestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        item.setHarvestId(harvestId);
        item.setWeightKg(120.5);

        ShipmentItemResponse response = ShipmentItemResponse.fromEntity(item);

        assertEquals(harvestId, response.harvestId());
        assertEquals(120.5, response.weightKg());
    }
}

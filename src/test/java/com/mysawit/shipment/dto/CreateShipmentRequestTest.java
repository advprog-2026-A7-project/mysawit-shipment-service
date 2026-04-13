package com.mysawit.shipment.dto;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class CreateShipmentRequestTest {

    @Test
    void recordStoresAllFields() {
        UUID supirUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID harvestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String destination = "Pabrik A";

        CreateShipmentRequest.HarvestItem item = new CreateShipmentRequest.HarvestItem(harvestId, 100.0);
        CreateShipmentRequest request = new CreateShipmentRequest(supirUserId, destination, List.of(item));

        assertEquals(supirUserId, request.supirUserId());
        assertEquals(destination, request.destination());
        assertNotNull(request.items());
        assertEquals(1, request.items().size());
        assertEquals(harvestId, request.items().get(0).harvestId());
        assertEquals(100.0, request.items().get(0).weightKg());
    }
}

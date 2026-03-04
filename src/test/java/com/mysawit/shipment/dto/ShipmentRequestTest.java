package com.mysawit.shipment.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentRequestTest {

    @Test
    void gettersAndSettersWork() {
        ShipmentRequest request = new ShipmentRequest();
        LocalDateTime shipmentDate = LocalDateTime.of(2026, 2, 1, 9, 30);
        LocalDateTime deliveryDate = LocalDateTime.of(2026, 2, 2, 9, 30);

        request.setHarvestId(1L);
        request.setDestination("Jakarta");
        request.setWeight(123.45);
        request.setStatus("PENDING");
        request.setShipperName("name");
        request.setVehicleNumber("B1234CD");
        request.setShipmentDate(shipmentDate);
        request.setDeliveryDate(deliveryDate);
        request.setNotes("notes");

        assertEquals(1L, request.getHarvestId());
        assertEquals("Jakarta", request.getDestination());
        assertEquals(123.45, request.getWeight());
        assertEquals("PENDING", request.getStatus());
        assertEquals("name", request.getShipperName());
        assertEquals("B1234CD", request.getVehicleNumber());
        assertEquals(shipmentDate, request.getShipmentDate());
        assertEquals(deliveryDate, request.getDeliveryDate());
        assertEquals("notes", request.getNotes());
    }
}

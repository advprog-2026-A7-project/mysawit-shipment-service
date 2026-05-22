package com.mysawit.shipment;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.dto.ShipmentRequest;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipmentCoverageTest {

    @Test
    void requestAndEntityAccessorsRoundTripValues() {
        LocalDateTime shippedAt = LocalDateTime.of(2026, 5, 22, 11, 0);
        LocalDateTime deliveredAt = shippedAt.plusHours(4);
        ShipmentRequest request = new ShipmentRequest();
        request.setHarvestId(101L);
        request.setDestination("Mill A");
        request.setWeight(900.5);
        request.setStatus("IN_TRANSIT");
        request.setShipperName("Budi");
        request.setVehicleNumber("B 1234 SAW");
        request.setShipmentDate(shippedAt);
        request.setDeliveryDate(deliveredAt);
        request.setNotes("Keep sealed");

        assertEquals(101L, request.getHarvestId());
        assertEquals("Mill A", request.getDestination());
        assertEquals(900.5, request.getWeight());
        assertEquals("IN_TRANSIT", request.getStatus());
        assertEquals("Budi", request.getShipperName());
        assertEquals("B 1234 SAW", request.getVehicleNumber());
        assertEquals(shippedAt, request.getShipmentDate());
        assertEquals(deliveredAt, request.getDeliveryDate());
        assertEquals("Keep sealed", request.getNotes());

        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setHarvestId(request.getHarvestId());
        shipment.setDestination(request.getDestination());
        shipment.setWeight(request.getWeight());
        shipment.setStatus(request.getStatus());
        shipment.setShipperName(request.getShipperName());
        shipment.setVehicleNumber(request.getVehicleNumber());
        shipment.setShipmentDate(request.getShipmentDate());
        shipment.setDeliveryDate(request.getDeliveryDate());
        shipment.setNotes(request.getNotes());
        shipment.setCreatedAt(shippedAt);
        shipment.setUpdatedAt(deliveredAt);

        assertEquals(1L, shipment.getId());
        assertEquals(101L, shipment.getHarvestId());
        assertEquals("Mill A", shipment.getDestination());
        assertEquals(900.5, shipment.getWeight());
        assertEquals("IN_TRANSIT", shipment.getStatus());
        assertEquals("Budi", shipment.getShipperName());
        assertEquals("B 1234 SAW", shipment.getVehicleNumber());
        assertEquals(shippedAt, shipment.getShipmentDate());
        assertEquals(deliveredAt, shipment.getDeliveryDate());
        assertEquals("Keep sealed", shipment.getNotes());
        assertEquals(shippedAt, shipment.getCreatedAt());
        assertEquals(deliveredAt, shipment.getUpdatedAt());
    }

    @Test
    void createEndpointMapsServiceRuntimeExceptionToBadRequest() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        ShipmentRequest request = new ShipmentRequest();
        when(shipmentService.createShipment(request)).thenThrow(new RuntimeException("invalid shipment"));

        ShipmentController controller = new ShipmentController(shipmentService);
        ResponseEntity<?> response = controller.createShipment(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}

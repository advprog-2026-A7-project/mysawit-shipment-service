package com.mysawit.shipment.controller;

import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentControllerTest {

    private ShipmentService shipmentService;
    private ShipmentController shipmentController;

    @BeforeEach
    void setUp() {
        shipmentService = mock(ShipmentService.class);
        shipmentController = new ShipmentController(shipmentService);
    }

    @Test
    void getAllShipmentsReturnsServiceResult() {
        when(shipmentService.getAllShipments()).thenReturn(List.of(sampleShipment(1L), sampleShipment(2L)));

        ResponseEntity<List<Shipment>> response = shipmentController.getAllShipments(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(shipmentService).getAllShipments();
    }

    @Test
    void getShipmentByIdReturnsShipment() {
        Shipment shipment = sampleShipment(1L);
        when(shipmentService.getShipmentById(1L)).thenReturn(shipment);

        ResponseEntity<?> response = shipmentController.getShipmentById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(shipment, response.getBody());
    }

    @Test
    void getShipmentByIdPropagatesRuntimeExceptionWhenMissing() {
        when(shipmentService.getShipmentById(1L)).thenThrow(new RuntimeException("missing"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shipmentController.getShipmentById(1L));

        assertEquals("missing", exception.getMessage());
    }

    @Test
    void healthReturnsUpStatus() {
        ResponseEntity<Map<String, String>> response = shipmentController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("mysawit-shipment-service", response.getBody().get("service"));
    }

    private Shipment sampleShipment(Long id) {
        Shipment shipment = new Shipment();
        shipment.setId(id);
        shipment.setHarvestId(10L);
        shipment.setSupirUserId(20L);
        shipment.setDestination("Jakarta");
        shipment.setTotalKg(100.0);
        shipment.setStatus("MEMUAT");
        return shipment;
    }
}

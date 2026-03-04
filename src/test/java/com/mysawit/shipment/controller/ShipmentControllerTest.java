package com.mysawit.shipment.controller;

import com.mysawit.shipment.dto.ShipmentRequest;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentControllerTest {

    private ShipmentService shipmentService;
    private ShipmentController shipmentController;

    @BeforeEach
    void setUp() {
        shipmentService = mock(ShipmentService.class);
        shipmentController = new ShipmentController(shipmentService);
    }

    @Test
    void getAllShipmentsUsesHarvestFilter() {
        Shipment shipment = sampleShipment(1L);
        when(shipmentService.getShipmentsByHarvestId(10L)).thenReturn(List.of(shipment));

        ResponseEntity<List<Shipment>> response = shipmentController.getAllShipments(10L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(shipmentService).getShipmentsByHarvestId(10L);
        verify(shipmentService, never()).getShipmentsByStatus(anyString());
        verify(shipmentService, never()).getAllShipments();
    }

    @Test
    void getAllShipmentsUsesStatusFilter() {
        Shipment shipment = sampleShipment(2L);
        when(shipmentService.getShipmentsByStatus("PENDING")).thenReturn(List.of(shipment));

        ResponseEntity<List<Shipment>> response = shipmentController.getAllShipments(null, "PENDING");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(shipmentService).getShipmentsByStatus("PENDING");
        verify(shipmentService, never()).getAllShipments();
    }

    @Test
    void getAllShipmentsReturnsAllWhenNoFilter() {
        when(shipmentService.getAllShipments()).thenReturn(List.of(sampleShipment(1L), sampleShipment(2L)));

        ResponseEntity<List<Shipment>> response = shipmentController.getAllShipments(null, null);

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
    void getShipmentByIdReturnsNotFoundWhenMissing() {
        when(shipmentService.getShipmentById(1L)).thenThrow(new RuntimeException("missing"));

        ResponseEntity<?> response = shipmentController.getShipmentById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void createShipmentReturnsCreated() {
        Shipment shipment = sampleShipment(1L);
        ShipmentRequest request = sampleRequest();
        when(shipmentService.createShipment(request)).thenReturn(shipment);

        ResponseEntity<?> response = shipmentController.createShipment(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(shipment, response.getBody());
    }

    @Test
    void createShipmentReturnsBadRequestOnError() {
        ShipmentRequest request = sampleRequest();
        when(shipmentService.createShipment(request)).thenThrow(new RuntimeException("invalid"));

        ResponseEntity<?> response = shipmentController.createShipment(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void updateShipmentReturnsUpdated() {
        Shipment shipment = sampleShipment(1L);
        ShipmentRequest request = sampleRequest();
        when(shipmentService.updateShipment(1L, request)).thenReturn(shipment);

        ResponseEntity<?> response = shipmentController.updateShipment(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(shipment, response.getBody());
    }

    @Test
    void updateShipmentReturnsNotFoundOnError() {
        ShipmentRequest request = sampleRequest();
        when(shipmentService.updateShipment(1L, request)).thenThrow(new RuntimeException("missing"));

        ResponseEntity<?> response = shipmentController.updateShipment(1L, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void deleteShipmentReturnsSuccessMessage() {
        ResponseEntity<?> response = shipmentController.deleteShipment(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Shipment deleted successfully", ((Map<?, ?>) response.getBody()).get("message"));
        verify(shipmentService).deleteShipment(1L);
    }

    @Test
    void deleteShipmentReturnsNotFoundOnError() {
        doThrow(new RuntimeException("missing")).when(shipmentService).deleteShipment(1L);

        ResponseEntity<?> response = shipmentController.deleteShipment(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", ((Map<?, ?>) response.getBody()).get("error"));
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
        shipment.setDestination("Jakarta");
        shipment.setWeight(100.0);
        shipment.setStatus("PENDING");
        shipment.setShipperName("Shipper");
        shipment.setVehicleNumber("B1234CD");
        shipment.setShipmentDate(LocalDateTime.now());
        shipment.setDeliveryDate(LocalDateTime.now().plusDays(1));
        shipment.setNotes("note");
        return shipment;
    }

    private ShipmentRequest sampleRequest() {
        ShipmentRequest request = new ShipmentRequest();
        request.setHarvestId(10L);
        request.setDestination("Jakarta");
        request.setWeight(100.0);
        request.setStatus("IN_TRANSIT");
        request.setShipperName("Shipper");
        request.setVehicleNumber("B1234CD");
        request.setShipmentDate(LocalDateTime.now());
        request.setDeliveryDate(LocalDateTime.now().plusDays(1));
        request.setNotes("note");
        return request;
    }
}

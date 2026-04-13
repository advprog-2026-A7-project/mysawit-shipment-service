package com.mysawit.shipment.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.dto.ShipmentResponse;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;

class ShipmentControllerTest {

    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private ShipmentService shipmentService;
    private ShipmentController shipmentController;

    @BeforeEach
    void setUp() {
        shipmentService = mock(ShipmentService.class);
        shipmentController = new ShipmentController(shipmentService);
    }

    @Test
    void getAllShipmentsReturnsServiceResult() {
        when(shipmentService.getAllShipments()).thenReturn(List.of(sampleShipment(ID_1), sampleShipment(ID_2)));

        ResponseEntity<List<ShipmentResponse>> response = shipmentController.getAllShipments(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(shipmentService).getAllShipments();
    }

    @Test
    void getShipmentByIdReturnsShipmentResponse() {
        Shipment shipment = sampleShipment(ID_1);
        when(shipmentService.getShipmentById(ID_1)).thenReturn(shipment);

        ResponseEntity<ShipmentResponse> response = shipmentController.getShipmentById(ID_1, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ID_1, response.getBody().id());
        assertEquals("Jakarta", response.getBody().destination());
        assertEquals(ShipmentStatus.MEMUAT, response.getBody().status());
    }

    @Test
    void getShipmentByIdPropagatesRuntimeExceptionWhenMissing() {
        when(shipmentService.getShipmentById(ID_1)).thenThrow(new RuntimeException("missing"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shipmentController.getShipmentById(ID_1, null));

        assertEquals("missing", exception.getMessage());
    }

    @Test
    void healthReturnsUpStatus() {
        ResponseEntity<Map<String, String>> response = shipmentController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("mysawit-shipment-service", response.getBody().get("service"));
    }

    private Shipment sampleShipment(UUID id) {
        Shipment shipment = new Shipment();
        shipment.setId(id);
        shipment.setHarvestId(UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111"));
        shipment.setSupirUserId(UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222"));
        shipment.setDestination("Jakarta");
        shipment.setTotalKg(100.0);
        shipment.setStatus(ShipmentStatus.MEMUAT);
        return shipment;
    }
}

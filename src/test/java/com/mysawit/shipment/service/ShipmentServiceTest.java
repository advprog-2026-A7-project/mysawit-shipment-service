package com.mysawit.shipment.service;

import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipmentServiceTest {

    private ShipmentRepository shipmentRepository;
    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        shipmentService = new ShipmentService(shipmentRepository);
    }

    @Test
    void getAllShipmentsReturnsRepositoryData() {
        when(shipmentRepository.findAll()).thenReturn(List.of(new Shipment(), new Shipment()));

        List<Shipment> result = shipmentService.getAllShipments();

        assertEquals(2, result.size());
    }

    @Test
    void getShipmentByIdReturnsEntity() {
        Shipment shipment = new Shipment();
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        Shipment result = shipmentService.getShipmentById(1L);

        assertSame(shipment, result);
    }

    @Test
    void getShipmentByIdThrowsWhenMissing() {
        when(shipmentRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shipmentService.getShipmentById(1L));

        assertEquals("Shipment not found with id: 1", exception.getMessage());
    }
}

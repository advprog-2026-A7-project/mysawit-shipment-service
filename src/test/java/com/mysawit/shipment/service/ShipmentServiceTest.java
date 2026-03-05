package com.mysawit.shipment.service;

import com.mysawit.shipment.domain.ShipmentStatus;
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
import static org.mockito.Mockito.verify;
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
    void getShipmentsBySupirUserIdReturnsRepositoryData() {
        when(shipmentRepository.findBySupirUserId(42L)).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsBySupirUserId(42L);

        assertEquals(1, result.size());
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

    @Test
    void updateShipmentStatusAllowsNextTransitionForOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(7L);
        shipment.setSupirUserId(42L);
        shipment.setStatus("MEMUAT");
        when(shipmentRepository.findById(7L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.updateShipmentStatus(7L, 42L, ShipmentStatus.MENGIRIM);

        assertSame(shipment, result);
        assertEquals("MENGIRIM", result.getStatus());
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void updateShipmentStatusRejectsSkippingTransition() {
        Shipment shipment = new Shipment();
        shipment.setId(8L);
        shipment.setSupirUserId(42L);
        shipment.setStatus("MEMUAT");
        when(shipmentRepository.findById(8L)).thenReturn(Optional.of(shipment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> shipmentService.updateShipmentStatus(8L, 42L, ShipmentStatus.TIBA)
        );

        assertEquals("Invalid status transition", exception.getMessage());
    }

    @Test
    void updateShipmentStatusRejectsTransitionFromTerminalStatus() {
        Shipment shipment = new Shipment();
        shipment.setId(9L);
        shipment.setSupirUserId(42L);
        shipment.setStatus("TIBA");
        when(shipmentRepository.findById(9L)).thenReturn(Optional.of(shipment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> shipmentService.updateShipmentStatus(9L, 42L, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Invalid status transition", exception.getMessage());
    }

    @Test
    void updateShipmentStatusRejectsWhenRequesterIsNotOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(10L);
        shipment.setSupirUserId(99L);
        shipment.setStatus("MEMUAT");
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> shipmentService.updateShipmentStatus(10L, 42L, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Forbidden", exception.getMessage());
    }
}

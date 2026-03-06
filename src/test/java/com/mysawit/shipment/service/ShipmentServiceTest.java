package com.mysawit.shipment.service;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentServiceTest {

    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID_4 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ID_7 = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID ID_8 = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID ID_9 = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID ID_10 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OWNER_42 = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final UUID OWNER_99 = UUID.fromString("99999999-4242-4242-4242-424242424242");

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
        when(shipmentRepository.findBySupirUserId(OWNER_42)).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsBySupirUserId(OWNER_42);

        assertEquals(1, result.size());
    }

    @Test
    void getShipmentByIdReturnsEntity() {
        Shipment shipment = new Shipment();
        when(shipmentRepository.findById(ID_1)).thenReturn(Optional.of(shipment));

        Shipment result = shipmentService.getShipmentById(ID_1);

        assertSame(shipment, result);
    }

    @Test
    void getShipmentByIdThrowsWhenMissing() {
        when(shipmentRepository.findById(ID_1)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shipmentService.getShipmentById(ID_1));

        assertEquals("Shipment not found with id: " + ID_1, exception.getMessage());
    }

    @Test
    void getShipmentByIdForSupirUserReturnsShipmentWhenOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_3);
        shipment.setSupirUserId(OWNER_42);
        when(shipmentRepository.findById(ID_3)).thenReturn(Optional.of(shipment));

        Shipment result = shipmentService.getShipmentByIdForSupirUser(ID_3, OWNER_42);

        assertSame(shipment, result);
    }

    @Test
    void getShipmentByIdForSupirUserThrowsForbiddenWhenNotOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_4);
        shipment.setSupirUserId(OWNER_99);
        when(shipmentRepository.findById(ID_4)).thenReturn(Optional.of(shipment));

        ShipmentForbiddenException exception = assertThrows(
                ShipmentForbiddenException.class,
                () -> shipmentService.getShipmentByIdForSupirUser(ID_4, OWNER_42)
        );

        assertEquals("Forbidden", exception.getMessage());
    }

    @Test
    void updateShipmentStatusAllowsNextTransitionForOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_7);
        shipment.setSupirUserId(OWNER_42);
        shipment.setStatus("MEMUAT");
        when(shipmentRepository.findById(ID_7)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.updateShipmentStatus(ID_7, OWNER_42, ShipmentStatus.MENGIRIM);

        assertSame(shipment, result);
        assertEquals("MENGIRIM", result.getStatus());
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void updateShipmentStatusRejectsSkippingTransition() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_8);
        shipment.setSupirUserId(OWNER_42);
        shipment.setStatus("MEMUAT");
        when(shipmentRepository.findById(ID_8)).thenReturn(Optional.of(shipment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> shipmentService.updateShipmentStatus(ID_8, OWNER_42, ShipmentStatus.TIBA)
        );

        assertEquals("Invalid status transition", exception.getMessage());
    }

    @Test
    void updateShipmentStatusRejectsTransitionFromTerminalStatus() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_9);
        shipment.setSupirUserId(OWNER_42);
        shipment.setStatus("TIBA");
        when(shipmentRepository.findById(ID_9)).thenReturn(Optional.of(shipment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> shipmentService.updateShipmentStatus(ID_9, OWNER_42, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Invalid status transition", exception.getMessage());
    }

    @Test
    void updateShipmentStatusRejectsWhenRequesterIsNotOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_10);
        shipment.setSupirUserId(OWNER_99);
        shipment.setStatus("MEMUAT");
        when(shipmentRepository.findById(ID_10)).thenReturn(Optional.of(shipment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> shipmentService.updateShipmentStatus(ID_10, OWNER_42, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Forbidden", exception.getMessage());
    }
}

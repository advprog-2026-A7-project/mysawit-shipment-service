package com.mysawit.shipment.service;

import com.mysawit.shipment.dto.ShipmentRequest;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void getShipmentsByHarvestIdReturnsRepositoryData() {
        when(shipmentRepository.findByHarvestId(10L)).thenReturn(List.of(new Shipment()));

        assertEquals(1, shipmentService.getShipmentsByHarvestId(10L).size());
    }

    @Test
    void getShipmentsByStatusReturnsRepositoryData() {
        when(shipmentRepository.findByStatus("PENDING")).thenReturn(List.of(new Shipment()));

        assertEquals(1, shipmentService.getShipmentsByStatus("PENDING").size());
    }

    @Test
    void createShipmentUsesProvidedStatus() {
        ShipmentRequest request = sampleRequest("IN_TRANSIT");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.createShipment(request);

        assertEquals("IN_TRANSIT", result.getStatus());
        assertEquals(10L, result.getHarvestId());
        assertEquals("Jakarta", result.getDestination());
        assertEquals(55.0, result.getWeight());
    }

    @Test
    void createShipmentUsesDefaultStatusWhenNull() {
        ShipmentRequest request = sampleRequest(null);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.createShipment(request);

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void updateShipmentUsesProvidedStatus() {
        Shipment existing = new Shipment();
        existing.setId(5L);
        when(shipmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.updateShipment(5L, sampleRequest("DELIVERED"));

        assertEquals("DELIVERED", result.getStatus());
        assertEquals("Jakarta", result.getDestination());
    }

    @Test
    void updateShipmentUsesDefaultStatusWhenNull() {
        Shipment existing = new Shipment();
        existing.setId(5L);
        when(shipmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.updateShipment(5L, sampleRequest(null));

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void updateShipmentThrowsWhenMissing() {
        when(shipmentRepository.findById(5L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shipmentService.updateShipment(5L, sampleRequest("LOW")));

        assertEquals("Shipment not found with id: 5", exception.getMessage());
    }

    @Test
    void deleteShipmentDeletesEntityWhenFound() {
        Shipment existing = new Shipment();
        when(shipmentRepository.findById(5L)).thenReturn(Optional.of(existing));

        shipmentService.deleteShipment(5L);

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).delete(captor.capture());
        assertSame(existing, captor.getValue());
    }

    @Test
    void deleteShipmentThrowsWhenMissing() {
        when(shipmentRepository.findById(5L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shipmentService.deleteShipment(5L));

        assertEquals("Shipment not found with id: 5", exception.getMessage());
        verify(shipmentRepository, never()).delete(any());
    }

    private ShipmentRequest sampleRequest(String status) {
        ShipmentRequest request = new ShipmentRequest();
        request.setHarvestId(10L);
        request.setDestination("Jakarta");
        request.setWeight(55.0);
        request.setStatus(status);
        request.setShipperName("Shipper");
        request.setVehicleNumber("B1234CD");
        request.setShipmentDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        request.setDeliveryDate(LocalDateTime.of(2026, 1, 2, 0, 0));
        request.setNotes("note");
        return request;
    }
}

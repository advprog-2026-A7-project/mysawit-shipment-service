package com.mysawit.shipment.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.shipment.client.HarvestServiceClient;
import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.dto.CreateShipmentRequest;
import com.mysawit.shipment.event.ShipmentEventPublisher;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.HarvestServiceUnavailableException;
import com.mysawit.shipment.exception.HarvestValidationException;
import com.mysawit.shipment.exception.ShipmentInvalidTransitionException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.exception.ShipmentWeightExceededException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.ShipmentItem;
import com.mysawit.shipment.repository.ShipmentRepository;

class ShipmentServiceTest {

    private static final String APPROVED_STATUS = "Approved";
    private static final UUID ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID_4 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ID_7 = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID ID_8 = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID ID_9 = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID ID_10 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ID_11 = UUID.fromString("abababab-abab-abab-abab-abababababab");
    private static final UUID OWNER_42 = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final UUID OWNER_99 = UUID.fromString("99999999-4242-4242-4242-424242424242");
    private static final UUID MANDOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID HARVEST_A = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID HARVEST_B = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID HARVEST_C = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final String DESTINATION = "Jakarta";

    private ShipmentRepository shipmentRepository;
    private HarvestServiceClient harvestServiceClient;
    private ShipmentEventPublisher shipmentEventPublisher;
    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        harvestServiceClient = mock(HarvestServiceClient.class);
        shipmentEventPublisher = mock(ShipmentEventPublisher.class);
        shipmentService = new ShipmentService(shipmentRepository, harvestServiceClient, shipmentEventPublisher);
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

        ShipmentNotFoundException exception = assertThrows(ShipmentNotFoundException.class, () -> shipmentService.getShipmentById(ID_1));

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
        shipment.setStatus(ShipmentStatus.MEMUAT);
        when(shipmentRepository.findById(ID_7)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.updateShipmentStatus(ID_7, OWNER_42, ShipmentStatus.MENGIRIM);

        assertSame(shipment, result);
        assertEquals(ShipmentStatus.MENGIRIM, result.getStatus());
        verify(shipmentRepository).save(shipment);
        verify(shipmentEventPublisher, never()).publishShipmentCompleted(shipment);
    }

    @Test
    void updateShipmentStatusPublishesEventWhenTransitioningToTiba() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_11);
        shipment.setSupirUserId(OWNER_42);
        shipment.setMandorUserId(MANDOR_ID);
        shipment.setTotalKg(320.0);
        shipment.setStatus(ShipmentStatus.MENGIRIM);
        shipment.getItems().add(shipmentItem(shipment, HARVEST_A, 200.0));
        shipment.getItems().add(shipmentItem(shipment, HARVEST_B, 120.0));
        when(shipmentRepository.findById(ID_11)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.updateShipmentStatus(ID_11, OWNER_42, ShipmentStatus.TIBA);

        assertSame(shipment, result);
        assertEquals(ShipmentStatus.TIBA, result.getStatus());
        verify(shipmentRepository).save(shipment);
        verify(shipmentEventPublisher).publishShipmentCompleted(shipment);
    }

    @Test
    void updateShipmentStatusRejectsSkippingTransition() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_8);
        shipment.setSupirUserId(OWNER_42);
        shipment.setStatus(ShipmentStatus.MEMUAT);
        when(shipmentRepository.findById(ID_8)).thenReturn(Optional.of(shipment));

        ShipmentInvalidTransitionException exception = assertThrows(
                ShipmentInvalidTransitionException.class,
                () -> shipmentService.updateShipmentStatus(ID_8, OWNER_42, ShipmentStatus.TIBA)
        );

        assertEquals("Invalid status transition", exception.getMessage());
    }

    @Test
    void updateShipmentStatusRejectsTransitionFromTerminalStatus() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_9);
        shipment.setSupirUserId(OWNER_42);
        shipment.setStatus(ShipmentStatus.TIBA);
        when(shipmentRepository.findById(ID_9)).thenReturn(Optional.of(shipment));

        ShipmentInvalidTransitionException exception = assertThrows(
                ShipmentInvalidTransitionException.class,
                () -> shipmentService.updateShipmentStatus(ID_9, OWNER_42, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Invalid status transition", exception.getMessage());
    }

    @Test
    void updateShipmentStatusRejectsWhenRequesterIsNotOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_10);
        shipment.setSupirUserId(OWNER_99);
        shipment.setStatus(ShipmentStatus.MEMUAT);
        when(shipmentRepository.findById(ID_10)).thenReturn(Optional.of(shipment));

        ShipmentForbiddenException exception = assertThrows(
                ShipmentForbiddenException.class,
                () -> shipmentService.updateShipmentStatus(ID_10, OWNER_42, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Forbidden", exception.getMessage());
    }

    @Test
    void createShipmentSavesEntityWithCalculatedTotalKg() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42, DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 150.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_B, 200.0)));

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A, HARVEST_B)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, APPROVED_STATUS),
                        HARVEST_B, new HarvestServiceClient.HarvestDetails(HARVEST_B, APPROVED_STATUS)
                ));
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_A)).thenReturn(false);
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_B)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.createShipment(MANDOR_ID, request);

        assertEquals(MANDOR_ID, result.getMandorUserId());
        assertEquals(OWNER_42, result.getSupirUserId());
        assertEquals(DESTINATION, result.getDestination());
        assertEquals(350.0, result.getTotalKg());
        assertEquals(ShipmentStatus.MEMUAT, result.getStatus());
        assertEquals(2, result.getItems().size());
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void createShipmentRejectsWhenTotalWeightExceeds400Kg() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42, DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 300.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_B, 150.0)));

        ShipmentWeightExceededException exception = assertThrows(
                ShipmentWeightExceededException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Total weight 450.0 kg exceeds maximum of 400 kg", exception.getMessage());
    }

    @Test
    void createShipmentAllowsExactly400Kg() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42, DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 200.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_B, 200.0)));

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A, HARVEST_B)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, APPROVED_STATUS),
                        HARVEST_B, new HarvestServiceClient.HarvestDetails(HARVEST_B, APPROVED_STATUS)
                ));
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_A)).thenReturn(false);
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_B)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.createShipment(MANDOR_ID, request);

        assertNotNull(result);
        assertEquals(400.0, result.getTotalKg());
    }

    @Test
    void createShipmentSetsItemBackReferences() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42, DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0)));

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, APPROVED_STATUS)
                ));
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_A)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.createShipment(MANDOR_ID, request);

        assertSame(result, result.getItems().get(0).getShipment());
        assertEquals(HARVEST_A, result.getItems().get(0).getHarvestId());
        assertEquals(100.0, result.getItems().get(0).getWeightKg());
    }

    @Test
    void createShipmentSucceedsWhenAllHarvestsAreApprovedAndUnclaimed() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(
                        new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_B, 120.0)
                )
        );

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A, HARVEST_B)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, APPROVED_STATUS),
                        HARVEST_B, new HarvestServiceClient.HarvestDetails(HARVEST_B, APPROVED_STATUS)
                ));
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_A)).thenReturn(false);
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_B)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.createShipment(MANDOR_ID, request);

        assertEquals(220.0, result.getTotalKg());
        verify(harvestServiceClient).getHarvestsByIds(eq(MANDOR_ID), eq(List.of(HARVEST_A, HARVEST_B)));
    }

    @Test
    void createShipmentFailsWhenOneOrMoreHarvestIdsAreInvalidOrNotFound() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(
                        new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_B, 120.0)
                )
        );

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A, HARVEST_B)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, APPROVED_STATUS)
                ));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest not found: " + HARVEST_B, exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenHarvestStatusIsNotApproved() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, "Pending")
                ));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest status must be " + APPROVED_STATUS + ": " + HARVEST_A, exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenHarvestWasAlreadyClaimedByAnotherShipment() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, APPROVED_STATUS)
                ));
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_A)).thenReturn(true);

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest already claimed: " + HARVEST_A, exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenHarvestServiceIsUnavailable() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A)))
                .thenThrow(new HarvestServiceUnavailableException("Harvest service is unavailable"));

        HarvestServiceUnavailableException exception = assertThrows(
                HarvestServiceUnavailableException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest service is unavailable", exception.getMessage());
    }

    @Test
    void createShipmentFailsForMixedValidAndInvalidHarvestIdsInTheSameRequest() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(
                        new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_C, 90.0)
                )
        );

        when(harvestServiceClient.getHarvestsByIds(MANDOR_ID, List.of(HARVEST_A, HARVEST_C)))
                .thenReturn(Map.of(
                        HARVEST_A, new HarvestServiceClient.HarvestDetails(HARVEST_A, APPROVED_STATUS)
                ));
        when(shipmentRepository.existsByItemsHarvestId(HARVEST_A)).thenReturn(false);

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest not found: " + HARVEST_C, exception.getMessage());
    }

    private ShipmentItem shipmentItem(Shipment shipment, UUID harvestId, double weightKg) {
        ShipmentItem item = new ShipmentItem();
        item.setShipment(shipment);
        item.setHarvestId(harvestId);
        item.setWeightKg(weightKg);
        return item;
    }
}

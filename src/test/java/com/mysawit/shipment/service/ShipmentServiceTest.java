package com.mysawit.shipment.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mysawit.shipment.service.HarvestReplicaService;
import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.dto.CreateShipmentRequest;
import com.mysawit.shipment.event.ShipmentEventPublisher;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.HarvestReplicaUnavailableException;
import com.mysawit.shipment.exception.HarvestValidationException;
import com.mysawit.shipment.exception.ShipmentInvalidTransitionException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.exception.ShipmentWeightExceededException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.ShipmentItem;
import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.ShipmentRepository;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;

class ShipmentServiceTest {

    private static final String APPROVED_STATUS = "Approved";
    private static final String HARVEST_NOT_FOUND_PREFIX = "Harvest not found: ";
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
    private static final String PLANTATION_ID = "plantation-1";
    private static final String OTHER_PLANTATION_ID = "plantation-2";
    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_SUPIR = "SUPIR";
    private static final String REASON_MISSING_FRUIT = "missing fruit";

    private ShipmentRepository shipmentRepository;
    private HarvestReplicaService harvestReplicaService;
    private ShipmentEventPublisher shipmentEventPublisher;
    private WorkerPlantationAssignmentRepository workerPlantationAssignmentRepository;
    private com.mysawit.shipment.service.WorkerAssignmentLookupService workerAssignmentLookup;
    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        harvestReplicaService = mock(HarvestReplicaService.class);
        shipmentEventPublisher = mock(ShipmentEventPublisher.class);
        workerPlantationAssignmentRepository = mock(WorkerPlantationAssignmentRepository.class);
        workerAssignmentLookup = mock(com.mysawit.shipment.service.WorkerAssignmentLookupService.class);
        shipmentService = new ShipmentService(
                shipmentRepository,
                harvestReplicaService,
                shipmentEventPublisher,
                workerPlantationAssignmentRepository,
                workerAssignmentLookup,
                400.0
        );
        when(workerAssignmentLookup.findByUserIdAndRole(MANDOR_ID, ROLE_MANDOR))
                .thenReturn(Optional.of(assignment(MANDOR_ID, ROLE_MANDOR, "Mandor One", PLANTATION_ID)));
        when(workerAssignmentLookup.findByUserIdAndRole(OWNER_42, ROLE_SUPIR))
                .thenReturn(Optional.of(assignment(OWNER_42, ROLE_SUPIR, "Supir One", PLANTATION_ID)));
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
    void getShipmentsBySupirUserIdWithFiltersDelegatesDateWindow() {
        LocalDate date = LocalDate.of(2026, 4, 14);
        when(shipmentRepository.findAll(anyShipmentSpecification())).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsBySupirUserId(
                OWNER_42,
                date,
                ShipmentStatus.MANDOR_REJECTED
        );

        assertEquals(1, result.size());
    }

    @Test
    void getShipmentsBySupirUserIdWithFiltersAllowsMissingFilters() {
        when(shipmentRepository.findAll(anyShipmentSpecification())).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsBySupirUserId(null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void getShipmentsByMandorUserIdWithFiltersTrimsBlankSupirName() {
        when(shipmentRepository.findAll(anyShipmentSpecification())).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsByMandorUserId(
                MANDOR_ID,
                OWNER_42,
                " ",
                null,
                ShipmentStatus.MEMUAT
        );

        assertEquals(1, result.size());
    }

    @Test
    void getShipmentsByMandorUserIdWithFiltersAllowsMissingIdsAndStatus() {
        when(shipmentRepository.findAll(anyShipmentSpecification())).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsByMandorUserId(
                null,
                null,
                " Supir ",
                null,
                null
        );

        assertEquals(1, result.size());
    }

    @Test
    void getShipmentsForAdminWithFiltersTrimsMandorName() {
        when(shipmentRepository.findAll(anyShipmentSpecification())).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsForAdmin(" Mandor ", null, ShipmentStatus.MANDOR_APPROVED);

        assertEquals(1, result.size());
    }

    @Test
    void getShipmentsForAdminDefaultsToMandorApprovedWhenStatusIsMissing() {
        when(shipmentRepository.findAll(anyShipmentSpecification())).thenReturn(List.of(new Shipment()));

        List<Shipment> result = shipmentService.getShipmentsForAdmin(null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void getSupirsForMandorReturnsSamePlantationSupirs() {
        WorkerPlantationAssignment supir = assignment(OWNER_42, ROLE_SUPIR, "Supir One", PLANTATION_ID);
        when(workerPlantationAssignmentRepository.findByRoleAndPlantationIdAndName(
                ROLE_SUPIR,
                PLANTATION_ID,
                "Supir"
        )).thenReturn(List.of(supir));

        List<WorkerPlantationAssignment> result = shipmentService.getSupirsForMandor(MANDOR_ID, " Supir ");

        assertEquals(1, result.size());
        assertSame(supir, result.get(0));
    }

    @Test
    void getShipmentByIdReturnsEntity() {
        Shipment shipment = new Shipment();
        when(shipmentRepository.findWithItemsById(ID_1)).thenReturn(Optional.of(shipment));

        Shipment result = shipmentService.getShipmentById(ID_1);

        assertSame(shipment, result);
    }

    @Test
    void getShipmentByIdThrowsWhenMissing() {
        when(shipmentRepository.findWithItemsById(ID_1)).thenReturn(Optional.empty());

        ShipmentNotFoundException exception = assertThrows(ShipmentNotFoundException.class, () -> shipmentService.getShipmentById(ID_1));

        assertEquals("Shipment not found with id: " + ID_1, exception.getMessage());
    }

    @Test
    void getShipmentByIdForSupirUserReturnsShipmentWhenOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_3);
        shipment.setSupirUserId(OWNER_42);
        when(shipmentRepository.findWithItemsById(ID_3)).thenReturn(Optional.of(shipment));

        Shipment result = shipmentService.getShipmentByIdForSupirUser(ID_3, OWNER_42);

        assertSame(shipment, result);
    }

    @Test
    void getShipmentByIdForSupirUserThrowsForbiddenWhenNotOwner() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_4);
        shipment.setSupirUserId(OWNER_99);
        when(shipmentRepository.findWithItemsById(ID_4)).thenReturn(Optional.of(shipment));

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
        when(shipmentRepository.findWithItemsById(ID_7)).thenReturn(Optional.of(shipment));
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
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));
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
        when(shipmentRepository.findWithItemsById(ID_8)).thenReturn(Optional.of(shipment));

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
        when(shipmentRepository.findWithItemsById(ID_9)).thenReturn(Optional.of(shipment));

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
        when(shipmentRepository.findWithItemsById(ID_10)).thenReturn(Optional.of(shipment));

        ShipmentForbiddenException exception = assertThrows(
                ShipmentForbiddenException.class,
                () -> shipmentService.updateShipmentStatus(ID_10, OWNER_42, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Forbidden", exception.getMessage());
    }

    @Test
    void approveShipmentByAdminUpdatesMandorApprovedShipmentToAdminApproved() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_11);
        shipment.setSupirUserId(OWNER_42);
        shipment.setMandorUserId(MANDOR_ID);
        shipment.setTotalKg(100.0);
        shipment.setStatus(ShipmentStatus.MANDOR_APPROVED);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.approveShipmentByAdmin(ID_11, ShipmentStatus.ADMIN_APPROVED);

        assertSame(shipment, result);
        assertEquals(ShipmentStatus.ADMIN_APPROVED, result.getStatus());
        assertEquals(100.0, result.getKgAccepted());
        verify(shipmentRepository).save(shipment);
        verify(shipmentEventPublisher).publishAdminApproved(shipment);
    }

    @Test
    void approveShipmentByAdminUpdatesMandorApprovedShipmentToPartiallyRejected() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_11);
        shipment.setSupirUserId(OWNER_42);
        shipment.setMandorUserId(MANDOR_ID);
        shipment.setTotalKg(100.0);
        shipment.setStatus(ShipmentStatus.MANDOR_APPROVED);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.approveShipmentByAdmin(
                ID_11,
                ShipmentStatus.PARTIALLY_REJECTED,
                REASON_MISSING_FRUIT,
                80.0
        );

        assertSame(shipment, result);
        assertEquals(ShipmentStatus.PARTIALLY_REJECTED, result.getStatus());
        assertEquals(80.0, result.getKgAccepted());
        assertEquals(REASON_MISSING_FRUIT, result.getRejectionReason());
        verify(shipmentRepository).save(shipment);
        verify(shipmentEventPublisher).publishAdminApproved(shipment);
    }

    @Test
    void approveShipmentByAdminRejectsUnsupportedDecision() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_11);
        shipment.setStatus(ShipmentStatus.TIBA);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentService.approveShipmentByAdmin(ID_11, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Invalid admin approval decision", exception.getMessage());
    }

    @Test
    void approveShipmentByAdminRejectsBeforeMandorApproval() {
        Shipment shipment = new Shipment();
        shipment.setId(ID_11);
        shipment.setStatus(ShipmentStatus.MENGIRIM);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        ShipmentInvalidTransitionException exception = assertThrows(
                ShipmentInvalidTransitionException.class,
                () -> shipmentService.approveShipmentByAdmin(ID_11, ShipmentStatus.ADMIN_APPROVED)
        );

        assertEquals("Shipment must be approved by Mandor before admin approval", exception.getMessage());
    }

    @Test
    void approveShipmentByMandorApprovesArrivedShipmentAndPublishesPayrollEvent() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.TIBA);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.approveShipmentByMandor(
                ID_11,
                MANDOR_ID,
                ShipmentStatus.MANDOR_APPROVED,
                null
        );

        assertEquals(ShipmentStatus.MANDOR_APPROVED, result.getStatus());
        verify(shipmentEventPublisher).publishMandorApproved(shipment);
    }

    @Test
    void approveShipmentByMandorRejectsWithReasonAndPublishesNotification() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.TIBA);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.approveShipmentByMandor(
                ID_11,
                MANDOR_ID,
                ShipmentStatus.MANDOR_REJECTED,
                "bad seal"
        );

        assertEquals(ShipmentStatus.MANDOR_REJECTED, result.getStatus());
        assertEquals("bad seal", result.getRejectionReason());
        verify(shipmentEventPublisher).publishMandorRejected(shipment);
    }

    @Test
    void approveShipmentByMandorRejectsWrongMandor() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.TIBA);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        ShipmentForbiddenException exception = assertThrows(
                ShipmentForbiddenException.class,
                () -> shipmentService.approveShipmentByMandor(
                        ID_11,
                        OWNER_99,
                        ShipmentStatus.MANDOR_APPROVED,
                        null
                )
        );

        assertEquals("Forbidden", exception.getMessage());
    }

    @Test
    void approveShipmentByMandorRequiresReasonForRejection() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.TIBA);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentService.approveShipmentByMandor(
                        ID_11,
                        MANDOR_ID,
                        ShipmentStatus.MANDOR_REJECTED,
                        " "
                )
        );

        assertEquals("Rejection reason is required", exception.getMessage());
    }

    @Test
    void approveShipmentByMandorRejectsInvalidDecision() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.TIBA);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        ShipmentInvalidTransitionException exception = assertThrows(
                ShipmentInvalidTransitionException.class,
                () -> shipmentService.approveShipmentByMandor(
                        ID_11,
                        MANDOR_ID,
                        ShipmentStatus.ADMIN_APPROVED,
                        null
                )
        );

        assertEquals("Invalid Mandor approval decision", exception.getMessage());
    }

    @Test
    void approveShipmentByMandorRequiresArrivedShipment() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.MENGIRIM);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        ShipmentInvalidTransitionException exception = assertThrows(
                ShipmentInvalidTransitionException.class,
                () -> shipmentService.approveShipmentByMandor(
                        ID_11,
                        MANDOR_ID,
                        ShipmentStatus.MANDOR_APPROVED,
                        null
                )
        );

        assertEquals("Shipment must be TIBA before Mandor approval", exception.getMessage());
    }

    @Test
    void approveShipmentByAdminRejectsShipmentWithReason() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.MANDOR_APPROVED);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Shipment result = shipmentService.approveShipmentByAdmin(
                ID_11,
                ShipmentStatus.ADMIN_REJECTED,
                "factory rejected",
                null
        );

        assertEquals(ShipmentStatus.ADMIN_REJECTED, result.getStatus());
        assertEquals(0.0, result.getKgAccepted());
        assertEquals("factory rejected", result.getRejectionReason());
        verify(shipmentEventPublisher).publishAdminRejected(shipment);
    }

    @Test
    void approveShipmentByAdminRequiresReasonForRejectedDecision() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.MANDOR_APPROVED);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentService.approveShipmentByAdmin(ID_11, ShipmentStatus.ADMIN_REJECTED, null, null)
        );

        assertEquals("Rejection reason is required", exception.getMessage());
    }

    @Test
    void approveShipmentByAdminRequiresKgForPartialRejection() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.MANDOR_APPROVED);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentService.approveShipmentByAdmin(
                        ID_11,
                        ShipmentStatus.PARTIALLY_REJECTED,
                        REASON_MISSING_FRUIT,
                        null
                )
        );

        assertEquals("Accepted kg is required for partial rejection", exception.getMessage());
    }

    @Test
    void approveShipmentByAdminRejectsPartialKgAboveTotal() {
        Shipment shipment = sampleReviewableShipment(ShipmentStatus.MANDOR_APPROVED);
        when(shipmentRepository.findWithItemsById(ID_11)).thenReturn(Optional.of(shipment));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentService.approveShipmentByAdmin(
                        ID_11,
                        ShipmentStatus.PARTIALLY_REJECTED,
                        REASON_MISSING_FRUIT,
                        500.0
                )
        );

        assertEquals("Accepted kg cannot exceed total shipment kg", exception.getMessage());
    }

    @Test
    void createShipmentSavesEntityWithCalculatedTotalKg() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42, DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 150.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_B, 200.0)));

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_B))
                .thenReturn(harvestDetails(HARVEST_B, APPROVED_STATUS));
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
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

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_B))
                .thenReturn(harvestDetails(HARVEST_B, APPROVED_STATUS));
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
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

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
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

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_B))
                .thenReturn(harvestDetails(HARVEST_B, APPROVED_STATUS));
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shipmentService.createShipment(MANDOR_ID, request);

        assertEquals(220.0, result.getTotalKg());
        verify(harvestReplicaService).getHarvestById(MANDOR_ID, HARVEST_A);
        verify(harvestReplicaService).getHarvestById(MANDOR_ID, HARVEST_B);
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

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_B))
                .thenThrow(new HarvestValidationException(
                        HARVEST_NOT_FOUND_PREFIX + HARVEST_B,
                        HttpStatus.NOT_FOUND
                ));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals(HARVEST_NOT_FOUND_PREFIX + HARVEST_B, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void createShipmentFailsWhenHarvestClientReturnsNullHarvest() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A)).thenReturn(null);

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals(HARVEST_NOT_FOUND_PREFIX + HARVEST_A, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void createShipmentFailsWhenHarvestStatusIsNotApproved() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, "Pending"));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest status must be " + APPROVED_STATUS + ": " + HARVEST_A, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void createShipmentFailsWhenHarvestStatusIsNull() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, null));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest status must be " + APPROVED_STATUS + ": " + HARVEST_A, exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenHarvestBelongsToDifferentMandor() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, OWNER_99, PLANTATION_ID, APPROVED_STATUS));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest does not belong to Mandor: " + HARVEST_A, exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenHarvestsComeFromMixedPlantations() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(
                        new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_B, 90.0)
                )
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_B))
                .thenReturn(harvestDetails(HARVEST_B, MANDOR_ID, OTHER_PLANTATION_ID, APPROVED_STATUS));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("All harvests must come from the same plantation", exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenMandorHasNoPlantationAssignment() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(workerAssignmentLookup.findByUserIdAndRole(MANDOR_ID, ROLE_MANDOR))
                .thenReturn(Optional.empty());

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Mandor is not assigned to a plantation", exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenMandorAssignmentDoesNotMatchHarvestPlantation() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(workerAssignmentLookup.findByUserIdAndRole(MANDOR_ID, ROLE_MANDOR))
                .thenReturn(Optional.of(assignment(MANDOR_ID, ROLE_MANDOR, "Mandor One", OTHER_PLANTATION_ID)));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Mandor must be assigned to the same plantation", exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenSupirHasNoPlantationAssignment() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(workerAssignmentLookup.findByUserIdAndRole(OWNER_42, ROLE_SUPIR))
                .thenReturn(Optional.empty());

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Supir is not assigned to a plantation", exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenSupirAssignmentDoesNotMatchHarvestPlantation() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(workerAssignmentLookup.findByUserIdAndRole(OWNER_42, ROLE_SUPIR))
                .thenReturn(Optional.of(assignment(OWNER_42, ROLE_SUPIR, "Supir One", OTHER_PLANTATION_ID)));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Supir must be assigned to the same plantation", exception.getMessage());
    }

    @Test
    void createShipmentFailsWhenHarvestWasAlreadyClaimedByAnotherShipment() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of(HARVEST_A));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest already claimed: " + HARVEST_A, exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void createShipmentFailsWhenConcurrentRequestClaimsHarvestDuringSave() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());
        when(shipmentRepository.save(any(Shipment.class)))
                .thenThrow(new DataIntegrityViolationException("uk_shipment_items_harvest_id"));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest already claimed by another shipment", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void createShipmentFailsWhenHarvestReplicaIsUnavailable() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0))
        );

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenThrow(new HarvestReplicaUnavailableException("Harvest data not yet replicated"));

        HarvestReplicaUnavailableException exception = assertThrows(
                HarvestReplicaUnavailableException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Harvest data not yet replicated", exception.getMessage());
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

        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_A))
                .thenReturn(harvestDetails(HARVEST_A, APPROVED_STATUS));
        when(harvestReplicaService.getHarvestById(MANDOR_ID, HARVEST_C))
                .thenThrow(new HarvestValidationException(
                        HARVEST_NOT_FOUND_PREFIX + HARVEST_C,
                        HttpStatus.NOT_FOUND
                ));
        when(shipmentRepository.findClaimedHarvestIds(any())).thenReturn(java.util.List.of());

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals(HARVEST_NOT_FOUND_PREFIX + HARVEST_C, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void createShipmentFailsWhenRequestContainsDuplicateHarvestIds() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                OWNER_42,
                DESTINATION,
                List.of(
                        new CreateShipmentRequest.HarvestItem(HARVEST_A, 100.0),
                        new CreateShipmentRequest.HarvestItem(HARVEST_A, 90.0)
                )
        );

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Duplicate harvest id in request: " + HARVEST_A, exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verifyNoInteractions(harvestReplicaService);
    }

    @Test
    void createShipmentRejectsEmptyItemsBeforePersisting() {
        CreateShipmentRequest request = new CreateShipmentRequest(OWNER_42, DESTINATION, List.of());

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> shipmentService.createShipment(MANDOR_ID, request)
        );

        assertEquals("Mandor must be assigned to the same plantation", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    private ShipmentItem shipmentItem(Shipment shipment, UUID harvestId, double weightKg) {
        ShipmentItem item = new ShipmentItem();
        item.setShipment(shipment);
        item.setHarvestId(harvestId);
        item.setWeightKg(weightKg);
        return item;
    }

    private Shipment sampleReviewableShipment(ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setId(ID_11);
        shipment.setMandorUserId(MANDOR_ID);
        shipment.setSupirUserId(OWNER_42);
        shipment.setDestination(DESTINATION);
        shipment.setPlantationId(PLANTATION_ID);
        shipment.setTotalKg(320.0);
        shipment.setStatus(status);
        shipment.getItems().add(shipmentItem(shipment, HARVEST_A, 200.0));
        return shipment;
    }

    private HarvestReplicaService.HarvestDetails harvestDetails(UUID harvestId, String status) {
        return new HarvestReplicaService.HarvestDetails(
                harvestId,
                MANDOR_ID,
                PLANTATION_ID,
                status,
                100.0
        );
    }

    private HarvestReplicaService.HarvestDetails harvestDetails(
            UUID harvestId,
            UUID mandorId,
            String plantationId,
            String status
    ) {
        return new HarvestReplicaService.HarvestDetails(
                harvestId,
                mandorId,
                plantationId,
                status,
                100.0
        );
    }

    private WorkerPlantationAssignment assignment(UUID userId, String role, String name, String plantationId) {
        WorkerPlantationAssignment assignment = new WorkerPlantationAssignment();
        assignment.setUserId(userId);
        assignment.setRole(role);
        assignment.setName(name);
        assignment.setPlantationId(plantationId);
        return assignment;
    }

    @SuppressWarnings("unchecked")
    private Specification<Shipment> anyShipmentSpecification() {
        return any(Specification.class);
    }
}

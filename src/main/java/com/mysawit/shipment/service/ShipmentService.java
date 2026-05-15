package com.mysawit.shipment.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysawit.shipment.client.HarvestServiceClient;
import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.domain.ShipmentStatusTransitionPolicy;
import com.mysawit.shipment.dto.CreateShipmentRequest;
import com.mysawit.shipment.event.ShipmentEventPublisher;
import com.mysawit.shipment.exception.HarvestValidationException;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.ShipmentInvalidTransitionException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.exception.ShipmentWeightExceededException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.ShipmentItem;
import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.ShipmentRepository;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;

@Service
public class ShipmentService {

    private static final String ERR_FORBIDDEN = "Forbidden";
    private static final String ERR_HARVEST_ALREADY_CLAIMED_PREFIX = "Harvest already claimed: ";
    private static final String ERR_HARVEST_DUPLICATE_PREFIX = "Duplicate harvest id in request: ";
    private static final String ERR_HARVEST_RACE_CLAIMED = "Harvest already claimed by another shipment";
    private static final String ERR_HARVEST_NOT_APPROVED_PREFIX = "Harvest status must be Approved: ";
    private static final String ERR_HARVEST_MANDOR_MISMATCH_PREFIX = "Harvest does not belong to Mandor: ";
    private static final String ERR_HARVEST_NOT_FOUND_PREFIX = "Harvest not found: ";
    private static final String ERR_INVALID_ADMIN_DECISION = "Invalid admin approval decision";
    private static final String ERR_INVALID_MANDOR_DECISION = "Invalid Mandor approval decision";
    private static final String ERR_INVALID_STATUS_TRANSITION = "Invalid status transition";
    private static final String ERR_KG_ACCEPTED_EXCEEDS_TOTAL = "Accepted kg cannot exceed total shipment kg";
    private static final String ERR_KG_ACCEPTED_REQUIRED = "Accepted kg is required for partial rejection";
    private static final String ERR_MANDOR_NOT_ASSIGNED = "Mandor is not assigned to a plantation";
    private static final String ERR_NOT_FOUND_PREFIX = "Shipment not found with id: ";
    private static final String ERR_REJECTION_REASON_REQUIRED = "Rejection reason is required";
    private static final String ERR_SHIPMENT_NOT_ARRIVED = "Shipment must be TIBA before Mandor approval";
    private static final String ERR_SHIPMENT_NOT_MANDOR_APPROVED = "Shipment must be approved by Mandor before admin approval";
    private static final String ERR_SUPIR_NOT_ASSIGNED = "Supir is not assigned to a plantation";
    private static final String ERR_SUPIR_NOT_SAME_PLANTATION = "Supir must be assigned to the same plantation";
    private static final String ERR_MANDOR_NOT_SAME_PLANTATION = "Mandor must be assigned to the same plantation";
    private static final String ERR_HARVEST_MIXED_PLANTATION = "All harvests must come from the same plantation";
    private static final String ERR_WEIGHT_EXCEEDED_FORMAT = "Total weight %s kg exceeds maximum of %.0f kg";
    private static final String REQUIRED_HARVEST_STATUS = "APPROVED";
    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_SUPIR = "SUPIR";
    
    private final HarvestServiceClient harvestServiceClient;
    private final ShipmentEventPublisher shipmentEventPublisher;
    private final ShipmentRepository shipmentRepository;
    private final WorkerPlantationAssignmentRepository workerPlantationAssignmentRepository;
    private final double maxWeightKg;
    
    public ShipmentService(
            ShipmentRepository shipmentRepository,
            HarvestServiceClient harvestServiceClient,
            ShipmentEventPublisher shipmentEventPublisher,
            WorkerPlantationAssignmentRepository workerPlantationAssignmentRepository,
            @Value("${shipment.max-weight-kg:400}") double maxWeightKg
    ) {
        this.shipmentRepository = shipmentRepository;
        this.harvestServiceClient = harvestServiceClient;
        this.shipmentEventPublisher = shipmentEventPublisher;
        this.workerPlantationAssignmentRepository = workerPlantationAssignmentRepository;
        this.maxWeightKg = maxWeightKg;
    }
    
    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    public List<Shipment> getShipmentsBySupirUserId(UUID supirUserId) {
        return shipmentRepository.findBySupirUserId(supirUserId);
    }

    public List<Shipment> getShipmentsBySupirUserId(UUID supirUserId, LocalDate date, ShipmentStatus status) {
        DateWindow dateWindow = DateWindow.from(date);
        return shipmentRepository.findWithFilters(
                supirUserId,
                null,
                status,
                null,
                null,
                dateWindow.from(),
                dateWindow.to()
        );
    }

    public List<Shipment> getShipmentsByMandorUserId(
            UUID mandorUserId,
            UUID supirUserId,
            String supirName,
            LocalDate date,
            ShipmentStatus status
    ) {
        DateWindow dateWindow = DateWindow.from(date);
        return shipmentRepository.findWithFilters(
                supirUserId,
                mandorUserId,
                status,
                null,
                trimToNull(supirName),
                dateWindow.from(),
                dateWindow.to()
        );
    }

    public List<Shipment> getShipmentsForAdmin(String mandorName, LocalDate date, ShipmentStatus status) {
        DateWindow dateWindow = DateWindow.from(date);
        ShipmentStatus effectiveStatus = status == null ? ShipmentStatus.MANDOR_APPROVED : status;
        return shipmentRepository.findWithFilters(
                null,
                null,
                effectiveStatus,
                trimToNull(mandorName),
                null,
                dateWindow.from(),
                dateWindow.to()
        );
    }

    public List<WorkerPlantationAssignment> getSupirsForMandor(UUID mandorUserId, String name) {
        WorkerPlantationAssignment mandorAssignment =
                resolveWorkerAssignment(mandorUserId, ROLE_MANDOR, ERR_MANDOR_NOT_ASSIGNED);
        return workerPlantationAssignmentRepository.findByRoleAndPlantationIdAndName(
                ROLE_SUPIR,
                mandorAssignment.getPlantationId(),
                trimToNull(name)
        );
    }
    
    public Shipment getShipmentById(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(ERR_NOT_FOUND_PREFIX + id));
    }

    public Shipment getShipmentByIdForSupirUser(UUID id, UUID requesterSupirUserId) {
        Shipment shipment = getShipmentById(id);
        ensureOwnedByRequester(shipment, requesterSupirUserId);
        return shipment;
    }

    @Transactional
    public Shipment updateShipmentStatus(UUID shipmentId, UUID requesterSupirUserId, ShipmentStatus targetStatus) {
        Shipment shipment = getShipmentById(shipmentId);
        ensureOwnedByRequester(shipment, requesterSupirUserId);
        ensureValidDriverStatusTransition(shipment, targetStatus);

        shipment.setStatus(targetStatus);
        Shipment savedShipment = shipmentRepository.save(shipment);
        if (ShipmentStatus.TIBA.equals(targetStatus)) {
            shipmentEventPublisher.publishShipmentCompleted(savedShipment);
        }
        return savedShipment;
    }

    @Transactional
    public Shipment approveShipmentByMandor(
            UUID shipmentId,
            UUID requesterMandorUserId,
            ShipmentStatus decision,
            String rejectionReason
    ) {
        Shipment shipment = getShipmentById(shipmentId);
        ensureOwnedByMandor(shipment, requesterMandorUserId);
        ensureValidMandorDecision(shipment, decision);

        if (decision == ShipmentStatus.MANDOR_REJECTED) {
            ensureReasonPresent(rejectionReason);
            shipment.setRejectionReason(rejectionReason);
        } else {
            shipment.setRejectionReason(null);
        }

        shipment.setStatus(decision);
        shipment.setMandorReviewedAt(OffsetDateTime.now());
        Shipment savedShipment = shipmentRepository.save(shipment);
        if (decision == ShipmentStatus.MANDOR_APPROVED) {
            shipmentEventPublisher.publishMandorApproved(savedShipment);
        } else {
            shipmentEventPublisher.publishMandorRejected(savedShipment);
        }
        return savedShipment;
    }

    @Transactional
    public Shipment approveShipmentByAdmin(UUID shipmentId, ShipmentStatus decision) {
        return approveShipmentByAdmin(shipmentId, decision, null, null);
    }

    @Transactional
    public Shipment approveShipmentByAdmin(
            UUID shipmentId,
            ShipmentStatus decision,
            String rejectionReason,
            Double kgAccepted
    ) {
        Shipment shipment = getShipmentById(shipmentId);
        ensureValidAdminDecision(decision);
        ensureShipmentMandorApprovedForAdminApproval(shipment);
        applyAdminDecision(shipment, decision, rejectionReason, kgAccepted);

        Shipment savedShipment = shipmentRepository.save(shipment);
        if (decision == ShipmentStatus.ADMIN_APPROVED || decision == ShipmentStatus.PARTIALLY_REJECTED) {
            shipmentEventPublisher.publishAdminApproved(savedShipment);
        } else {
            shipmentEventPublisher.publishAdminRejected(savedShipment);
        }
        return savedShipment;
    }

    @Transactional
    public Shipment createShipment(UUID mandorUserId, CreateShipmentRequest request) {
        double totalKg = calculateTotalKg(request);
        ensureWithinWeightLimit(totalKg);
        AssignmentPair assignments = validateHarvests(mandorUserId, request);

        Shipment shipment = buildShipment(mandorUserId, request, totalKg, assignments);
        return saveShipment(shipment);
    }

    private void ensureWithinWeightLimit(double totalKg) {
        if (totalKg > maxWeightKg) {
            throw new ShipmentWeightExceededException(String.format(
                    Locale.ROOT,
                    ERR_WEIGHT_EXCEEDED_FORMAT,
                    Double.toString(totalKg),
                    maxWeightKg
            ));
        }
    }

    private Shipment buildShipment(
            UUID mandorUserId,
            CreateShipmentRequest request,
            double totalKg,
            AssignmentPair assignments
    ) {
        Shipment shipment = new Shipment();
        shipment.setMandorUserId(mandorUserId);
        shipment.setMandorName(assignments.mandor().getName());
        shipment.setSupirUserId(request.supirUserId());
        shipment.setSupirName(assignments.supir().getName());
        shipment.setPlantationId(assignments.mandor().getPlantationId());
        shipment.setDestination(request.destination());
        shipment.setTotalKg(totalKg);

        for (CreateShipmentRequest.HarvestItem item : request.items()) {
            shipment.addItem(toShipmentItem(item));
        }

        return shipment;
    }

    private Shipment saveShipment(Shipment shipment) {
        try {
            return shipmentRepository.save(shipment);
        } catch (DataIntegrityViolationException ex) {
            throw HarvestValidationException.conflict(ERR_HARVEST_RACE_CLAIMED);
        }
    }

    private double calculateTotalKg(CreateShipmentRequest request) {
        return request.items().stream()
                .mapToDouble(CreateShipmentRequest.HarvestItem::weightKg)
                .sum();
    }

    private AssignmentPair validateHarvests(UUID mandorUserId, CreateShipmentRequest request) {
        ensureUniqueHarvestIds(request.items());
        String plantationId = null;
        for (CreateShipmentRequest.HarvestItem item : request.items()) {
            UUID harvestId = item.harvestId();
            HarvestServiceClient.HarvestDetails harvest = harvestServiceClient.getHarvestById(mandorUserId, harvestId);
            validateHarvest(mandorUserId, harvestId, harvest);
            plantationId = resolveShipmentPlantation(plantationId, harvest.plantationId());
        }
        return ensureSamePlantationAssignments(mandorUserId, request.supirUserId(), plantationId);
    }

    private void ensureUniqueHarvestIds(List<CreateShipmentRequest.HarvestItem> items) {
        Set<UUID> seenHarvestIds = new HashSet<>();
        for (CreateShipmentRequest.HarvestItem item : items) {
            UUID harvestId = item.harvestId();
            if (!seenHarvestIds.add(harvestId)) {
                throw HarvestValidationException.conflict(ERR_HARVEST_DUPLICATE_PREFIX + harvestId);
            }
        }
    }

    private void validateHarvest(UUID mandorUserId, UUID harvestId, HarvestServiceClient.HarvestDetails harvest) {
        if (harvest == null) {
            throw HarvestValidationException.notFound(ERR_HARVEST_NOT_FOUND_PREFIX + harvestId);
        }
        if (!Objects.equals(mandorUserId, harvest.mandorUserId())) {
            throw HarvestValidationException.badRequest(ERR_HARVEST_MANDOR_MISMATCH_PREFIX + harvestId);
        }
        if (!REQUIRED_HARVEST_STATUS.equals(normalizeHarvestStatus(harvest.status()))) {
            throw HarvestValidationException.badRequest(ERR_HARVEST_NOT_APPROVED_PREFIX + harvestId);
        }
        if (shipmentRepository.existsByItemsHarvestId(harvestId)) {
            throw HarvestValidationException.conflict(ERR_HARVEST_ALREADY_CLAIMED_PREFIX + harvestId);
        }
    }

    private String resolveShipmentPlantation(String currentPlantationId, String harvestPlantationId) {
        if (currentPlantationId == null) {
            return harvestPlantationId;
        }
        if (!Objects.equals(currentPlantationId, harvestPlantationId)) {
            throw HarvestValidationException.badRequest(ERR_HARVEST_MIXED_PLANTATION);
        }
        return currentPlantationId;
    }

    private AssignmentPair ensureSamePlantationAssignments(UUID mandorUserId, UUID supirUserId, String shipmentPlantationId) {
        WorkerPlantationAssignment mandorAssignment =
                resolveWorkerAssignment(mandorUserId, ROLE_MANDOR, ERR_MANDOR_NOT_ASSIGNED);
        WorkerPlantationAssignment supirAssignment =
                resolveWorkerAssignment(supirUserId, ROLE_SUPIR, ERR_SUPIR_NOT_ASSIGNED);

        if (!Objects.equals(mandorAssignment.getPlantationId(), shipmentPlantationId)) {
            throw HarvestValidationException.badRequest(ERR_MANDOR_NOT_SAME_PLANTATION);
        }
        if (!Objects.equals(supirAssignment.getPlantationId(), shipmentPlantationId)) {
            throw HarvestValidationException.badRequest(ERR_SUPIR_NOT_SAME_PLANTATION);
        }
        return new AssignmentPair(mandorAssignment, supirAssignment);
    }

    private WorkerPlantationAssignment resolveWorkerAssignment(UUID userId, String role, String missingMessage) {
        return workerPlantationAssignmentRepository.findByUserIdAndRole(userId, role)
                .orElseThrow(() -> HarvestValidationException.badRequest(missingMessage));
    }

    private String normalizeHarvestStatus(String status) {
        return status == null ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    private ShipmentItem toShipmentItem(CreateShipmentRequest.HarvestItem item) {
        ShipmentItem shipmentItem = new ShipmentItem();
        shipmentItem.setHarvestId(item.harvestId());
        shipmentItem.setWeightKg(item.weightKg());
        return shipmentItem;
    }

    private void ensureOwnedByRequester(Shipment shipment, UUID requesterSupirUserId) {
        if (!Objects.equals(shipment.getSupirUserId(), requesterSupirUserId)) {
            throw new ShipmentForbiddenException(ERR_FORBIDDEN);
        }
    }

    private void ensureOwnedByMandor(Shipment shipment, UUID requesterMandorUserId) {
        if (!Objects.equals(shipment.getMandorUserId(), requesterMandorUserId)) {
            throw new ShipmentForbiddenException(ERR_FORBIDDEN);
        }
    }

    private void ensureValidDriverStatusTransition(Shipment shipment, ShipmentStatus targetStatus) {
        ShipmentStatus currentStatus = shipment.getStatus();
        if (!ShipmentStatusTransitionPolicy.canDriverTransition(currentStatus, targetStatus)) {
            throw new ShipmentInvalidTransitionException(ERR_INVALID_STATUS_TRANSITION);
        }
    }

    private void ensureValidMandorDecision(Shipment shipment, ShipmentStatus decision) {
        if (!ShipmentStatusTransitionPolicy.canMandorDecision(shipment.getStatus(), decision)) {
            throw new ShipmentInvalidTransitionException(
                    shipment.getStatus() == ShipmentStatus.TIBA ? ERR_INVALID_MANDOR_DECISION : ERR_SHIPMENT_NOT_ARRIVED
            );
        }
    }

    private void ensureValidAdminDecision(ShipmentStatus decision) {
        if (decision != ShipmentStatus.ADMIN_APPROVED
                && decision != ShipmentStatus.ADMIN_REJECTED
                && decision != ShipmentStatus.PARTIALLY_REJECTED) {
            throw new IllegalArgumentException(ERR_INVALID_ADMIN_DECISION);
        }
    }

    private void ensureShipmentMandorApprovedForAdminApproval(Shipment shipment) {
        if (!ShipmentStatusTransitionPolicy.canAdminDecision(shipment.getStatus(), ShipmentStatus.ADMIN_APPROVED)) {
            throw new ShipmentInvalidTransitionException(ERR_SHIPMENT_NOT_MANDOR_APPROVED);
        }
    }

    private void applyAdminDecision(
            Shipment shipment,
            ShipmentStatus decision,
            String rejectionReason,
            Double kgAccepted
    ) {
        if (decision == ShipmentStatus.ADMIN_APPROVED) {
            shipment.setKgAccepted(shipment.getTotalKg());
            shipment.setRejectionReason(null);
        } else if (decision == ShipmentStatus.ADMIN_REJECTED) {
            ensureReasonPresent(rejectionReason);
            shipment.setKgAccepted(0.0);
            shipment.setRejectionReason(rejectionReason);
        } else {
            ensureReasonPresent(rejectionReason);
            ensurePartialAcceptedKg(shipment, kgAccepted);
            shipment.setKgAccepted(kgAccepted);
            shipment.setRejectionReason(rejectionReason);
        }
        shipment.setStatus(decision);
        shipment.setAdminReviewedAt(OffsetDateTime.now());
    }

    private void ensurePartialAcceptedKg(Shipment shipment, Double kgAccepted) {
        if (kgAccepted == null) {
            throw new IllegalArgumentException(ERR_KG_ACCEPTED_REQUIRED);
        }
        if (kgAccepted > shipment.getTotalKg()) {
            throw new IllegalArgumentException(ERR_KG_ACCEPTED_EXCEEDS_TOTAL);
        }
    }

    private void ensureReasonPresent(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException(ERR_REJECTION_REASON_REQUIRED);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record AssignmentPair(
            WorkerPlantationAssignment mandor,
            WorkerPlantationAssignment supir
    ) {
    }

    private record DateWindow(OffsetDateTime from, OffsetDateTime to) {

        private static DateWindow from(LocalDate date) {
            if (date == null) {
                return new DateWindow(null, null);
            }
            OffsetDateTime start = date.atStartOfDay().atOffset(ZoneOffset.UTC);
            return new DateWindow(start, start.plusDays(1));
        }
    }
}

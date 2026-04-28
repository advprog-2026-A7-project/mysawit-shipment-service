package com.mysawit.shipment.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
import com.mysawit.shipment.repository.ShipmentRepository;

@Service
public class ShipmentService {

    private static final String ERR_FORBIDDEN = "Forbidden";
    private static final String ERR_HARVEST_ALREADY_CLAIMED_PREFIX = "Harvest already claimed: ";
    private static final String ERR_HARVEST_DUPLICATE_PREFIX = "Duplicate harvest id in request: ";
    private static final String ERR_HARVEST_RACE_CLAIMED = "Harvest already claimed by another shipment";
    private static final String ERR_HARVEST_NOT_APPROVED_PREFIX = "Harvest status must be Approved: ";
    private static final String ERR_HARVEST_NOT_FOUND_PREFIX = "Harvest not found: ";
    private static final String ERR_INVALID_ADMIN_DECISION = "Invalid admin approval decision";
    private static final String ERR_INVALID_STATUS_TRANSITION = "Invalid status transition";
    private static final String ERR_NOT_FOUND_PREFIX = "Shipment not found with id: ";
    private static final String ERR_SHIPMENT_NOT_ARRIVED = "Shipment must be TIBA before admin approval";
    private static final String ERR_WEIGHT_EXCEEDED_FORMAT = "Total weight %s kg exceeds maximum of %.0f kg";
    private static final String REQUIRED_HARVEST_STATUS = "Approved";
    private static final double MAX_WEIGHT_KG = 400.0;
    
    private final HarvestServiceClient harvestServiceClient;
    private final ShipmentEventPublisher shipmentEventPublisher;
    private final ShipmentRepository shipmentRepository;
    
    public ShipmentService(
            ShipmentRepository shipmentRepository,
            HarvestServiceClient harvestServiceClient,
            ShipmentEventPublisher shipmentEventPublisher
    ) {
        this.shipmentRepository = shipmentRepository;
        this.harvestServiceClient = harvestServiceClient;
        this.shipmentEventPublisher = shipmentEventPublisher;
    }
    
    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    public List<Shipment> getShipmentsBySupirUserId(UUID supirUserId) {
        return shipmentRepository.findBySupirUserId(supirUserId);
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
        ensureValidStatusTransition(shipment, targetStatus);

        shipment.setStatus(targetStatus);
        Shipment savedShipment = shipmentRepository.save(shipment);
        if (ShipmentStatus.TIBA.equals(targetStatus)) {
            shipmentEventPublisher.publishShipmentCompleted(savedShipment);
        }
        return savedShipment;
    }

    @Transactional
    public Shipment approveShipmentByAdmin(UUID shipmentId, ShipmentStatus decision) {
        Shipment shipment = getShipmentById(shipmentId);
        ensureValidAdminDecision(decision);
        ensureShipmentArrivedForAdminApproval(shipment);

        shipment.setStatus(decision);
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment createShipment(UUID mandorUserId, CreateShipmentRequest request) {
        double totalKg = calculateTotalKg(request);
        ensureWithinWeightLimit(totalKg);
        validateHarvests(mandorUserId, request);

        Shipment shipment = buildShipment(mandorUserId, request, totalKg);
        return saveShipment(shipment);
    }

    private void ensureWithinWeightLimit(double totalKg) {
        if (totalKg > MAX_WEIGHT_KG) {
            throw new ShipmentWeightExceededException(String.format(
                    Locale.ROOT,
                    ERR_WEIGHT_EXCEEDED_FORMAT,
                    Double.toString(totalKg),
                    MAX_WEIGHT_KG
            ));
        }
    }

    private Shipment buildShipment(UUID mandorUserId, CreateShipmentRequest request, double totalKg) {
        Shipment shipment = new Shipment();
        shipment.setMandorUserId(mandorUserId);
        shipment.setSupirUserId(request.supirUserId());
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

    private void validateHarvests(UUID mandorUserId, CreateShipmentRequest request) {
        ensureUniqueHarvestIds(request.items());
        for (CreateShipmentRequest.HarvestItem item : request.items()) {
            UUID harvestId = item.harvestId();
            validateHarvest(harvestId, harvestServiceClient.getHarvestById(mandorUserId, harvestId));
        }
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

    private void validateHarvest(UUID harvestId, HarvestServiceClient.HarvestDetails harvest) {
        if (harvest == null) {
            throw HarvestValidationException.notFound(ERR_HARVEST_NOT_FOUND_PREFIX + harvestId);
        }
        if (!REQUIRED_HARVEST_STATUS.equals(harvest.status())) {
            throw HarvestValidationException.badRequest(ERR_HARVEST_NOT_APPROVED_PREFIX + harvestId);
        }
        if (shipmentRepository.existsByItemsHarvestId(harvestId)) {
            throw HarvestValidationException.conflict(ERR_HARVEST_ALREADY_CLAIMED_PREFIX + harvestId);
        }
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

    private void ensureValidStatusTransition(Shipment shipment, ShipmentStatus targetStatus) {
        ShipmentStatus currentStatus = shipment.getStatus();
        if (!ShipmentStatusTransitionPolicy.canTransition(currentStatus, targetStatus)) {
            throw new ShipmentInvalidTransitionException(ERR_INVALID_STATUS_TRANSITION);
        }
    }

    private void ensureValidAdminDecision(ShipmentStatus decision) {
        if (decision != ShipmentStatus.ADMIN_APPROVED && decision != ShipmentStatus.PARTIALLY_REJECTED) {
            throw new IllegalArgumentException(ERR_INVALID_ADMIN_DECISION);
        }
    }

    private void ensureShipmentArrivedForAdminApproval(Shipment shipment) {
        if (shipment.getStatus() != ShipmentStatus.TIBA) {
            throw new ShipmentInvalidTransitionException(ERR_SHIPMENT_NOT_ARRIVED);
        }
    }
}

package com.mysawit.shipment.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.domain.ShipmentStatusTransitionPolicy;
import com.mysawit.shipment.dto.CreateShipmentRequest;
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
    private static final String ERR_INVALID_STATUS_TRANSITION = "Invalid status transition";
    private static final String ERR_NOT_FOUND_PREFIX = "Shipment not found with id: ";
    private static final double MAX_WEIGHT_KG = 400.0;
    
    private final ShipmentRepository shipmentRepository;
    
    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
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
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment createShipment(UUID mandorUserId, CreateShipmentRequest request) {
        double totalKg = request.items().stream()
                .mapToDouble(CreateShipmentRequest.HarvestItem::weightKg)
                .sum();

        if (totalKg > MAX_WEIGHT_KG) {
            throw new ShipmentWeightExceededException(
                    "Total weight " + totalKg + " kg exceeds maximum of 400 kg");
        }

        Shipment shipment = new Shipment();
        shipment.setMandorUserId(mandorUserId);
        shipment.setSupirUserId(request.supirUserId());
        shipment.setDestination(request.destination());
        shipment.setTotalKg(totalKg);

        for (CreateShipmentRequest.HarvestItem item : request.items()) {
            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setHarvestId(item.harvestId());
            shipmentItem.setWeightKg(item.weightKg());
            shipmentItem.setShipment(shipment);
            shipment.getItems().add(shipmentItem);
        }

        return shipmentRepository.save(shipment);
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
}

package com.mysawit.shipment.service;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.domain.ShipmentStatusTransitionPolicy;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.ShipmentInvalidTransitionException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ShipmentService {

    private static final String ERR_FORBIDDEN = "Forbidden";
    private static final String ERR_INVALID_STATUS_TRANSITION = "Invalid status transition";
    private static final String ERR_NOT_FOUND_PREFIX = "Shipment not found with id: ";
    
    private final ShipmentRepository shipmentRepository;
    
    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }
    
    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    public List<Shipment> getShipmentsBySupirUserId(Long supirUserId) {
        return shipmentRepository.findBySupirUserId(supirUserId);
    }
    
    public Shipment getShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(ERR_NOT_FOUND_PREFIX + id));
    }

    public Shipment getShipmentByIdForSupirUser(Long id, Long requesterSupirUserId) {
        Shipment shipment = getShipmentById(id);
        ensureOwnedByRequester(shipment, requesterSupirUserId);
        return shipment;
    }

    public Shipment updateShipmentStatus(Long shipmentId, Long requesterSupirUserId, ShipmentStatus targetStatus) {
        Shipment shipment = getShipmentById(shipmentId);
        ensureOwnedByRequester(shipment, requesterSupirUserId);
        ensureValidStatusTransition(shipment, targetStatus);

        shipment.setStatus(targetStatus.name());
        return shipmentRepository.save(shipment);
    }

    private void ensureOwnedByRequester(Shipment shipment, Long requesterSupirUserId) {
        if (!Objects.equals(shipment.getSupirUserId(), requesterSupirUserId)) {
            throw new ShipmentForbiddenException(ERR_FORBIDDEN);
        }
    }

    private void ensureValidStatusTransition(Shipment shipment, ShipmentStatus targetStatus) {
        ShipmentStatus currentStatus = ShipmentStatus.valueOf(shipment.getStatus());
        if (!ShipmentStatusTransitionPolicy.canTransition(currentStatus, targetStatus)) {
            throw new ShipmentInvalidTransitionException(ERR_INVALID_STATUS_TRANSITION);
        }
    }
}

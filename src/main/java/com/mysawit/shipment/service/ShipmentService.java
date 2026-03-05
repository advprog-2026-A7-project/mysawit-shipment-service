package com.mysawit.shipment.service;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.domain.ShipmentStatusTransitionPolicy;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentService {
    
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
                .orElseThrow(() -> new RuntimeException("Shipment not found with id: " + id));
    }

    public Shipment updateShipmentStatus(Long shipmentId, Long requesterSupirUserId, ShipmentStatus targetStatus) {
        Shipment shipment = getShipmentById(shipmentId);
        if (!shipment.getSupirUserId().equals(requesterSupirUserId)) {
            throw new RuntimeException("Forbidden");
        }

        ShipmentStatus currentStatus = ShipmentStatus.valueOf(shipment.getStatus());
        if (!ShipmentStatusTransitionPolicy.canTransition(currentStatus, targetStatus)) {
            throw new RuntimeException("Invalid status transition");
        }

        shipment.setStatus(targetStatus.name());
        return shipmentRepository.save(shipment);
    }
}

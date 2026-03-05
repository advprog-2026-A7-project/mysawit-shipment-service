package com.mysawit.shipment.service;

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
    
    public Shipment getShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found with id: " + id));
    }
}

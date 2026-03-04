package com.mysawit.shipment.service;

import com.mysawit.shipment.dto.ShipmentRequest;
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
    
    public List<Shipment> getShipmentsByHarvestId(Long harvestId) {
        return shipmentRepository.findByHarvestId(harvestId);
    }
    
    public List<Shipment> getShipmentsByStatus(String status) {
        return shipmentRepository.findByStatus(status);
    }
    
    public Shipment createShipment(ShipmentRequest request) {
        Shipment shipment = new Shipment();
        shipment.setHarvestId(request.getHarvestId());
        shipment.setDestination(request.getDestination());
        shipment.setWeight(request.getWeight());
        shipment.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        shipment.setShipperName(request.getShipperName());
        shipment.setVehicleNumber(request.getVehicleNumber());
        shipment.setShipmentDate(request.getShipmentDate());
        shipment.setDeliveryDate(request.getDeliveryDate());
        shipment.setNotes(request.getNotes());
        
        return shipmentRepository.save(shipment);
    }
    
    public Shipment updateShipment(Long id, ShipmentRequest request) {
        Shipment shipment = getShipmentById(id);
        
        shipment.setHarvestId(request.getHarvestId());
        shipment.setDestination(request.getDestination());
        shipment.setWeight(request.getWeight());
        shipment.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        shipment.setShipperName(request.getShipperName());
        shipment.setVehicleNumber(request.getVehicleNumber());
        shipment.setShipmentDate(request.getShipmentDate());
        shipment.setDeliveryDate(request.getDeliveryDate());
        shipment.setNotes(request.getNotes());
        
        return shipmentRepository.save(shipment);
    }
    
    public void deleteShipment(Long id) {
        Shipment shipment = getShipmentById(id);
        shipmentRepository.delete(shipment);
    }
}

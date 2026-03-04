package com.mysawit.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public class ShipmentRequest {
    
    @NotNull(message = "Harvest ID is required")
    private Long harvestId;
    
    @NotBlank(message = "Destination is required")
    private String destination;
    
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private Double weight;
    
    private String status;
    private String shipperName;
    private String vehicleNumber;
    private LocalDateTime shipmentDate;
    private LocalDateTime deliveryDate;
    private String notes;
    
    // Getters and Setters
    public Long getHarvestId() {
        return harvestId;
    }
    
    public void setHarvestId(Long harvestId) {
        this.harvestId = harvestId;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getShipperName() {
        return shipperName;
    }
    
    public void setShipperName(String shipperName) {
        this.shipperName = shipperName;
    }
    
    public String getVehicleNumber() {
        return vehicleNumber;
    }
    
    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
    
    public LocalDateTime getShipmentDate() {
        return shipmentDate;
    }
    
    public void setShipmentDate(LocalDateTime shipmentDate) {
        this.shipmentDate = shipmentDate;
    }
    
    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }
    
    public void setDeliveryDate(LocalDateTime deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}

package com.mysawit.shipment.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Harvest ID is required")
    @Column(name = "harvest_id", nullable = false)
    private Long harvestId;

    @NotNull(message = "Supir user ID is required")
    @Column(name = "supir_user_id", nullable = false)
    private Long supirUserId;
    
    @NotBlank(message = "Destination is required")
    @Column(nullable = false)
    private String destination;
    
    @NotNull(message = "Total kg is required")
    @Positive(message = "Total kg must be positive")
    @Column(name = "total_kg", nullable = false)
    private Double totalKg;
    
    @Column(nullable = false)
    private String status = "MEMUAT";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getHarvestId() {
        return harvestId;
    }
    
    public void setHarvestId(Long harvestId) {
        this.harvestId = harvestId;
    }

    public Long getSupirUserId() {
        return supirUserId;
    }

    public void setSupirUserId(Long supirUserId) {
        this.supirUserId = supirUserId;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public Double getTotalKg() {
        return totalKg;
    }
    
    public void setTotalKg(Double totalKg) {
        this.totalKg = totalKg;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

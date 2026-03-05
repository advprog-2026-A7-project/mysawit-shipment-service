package com.mysawit.shipment.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Getter
@Setter
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
}

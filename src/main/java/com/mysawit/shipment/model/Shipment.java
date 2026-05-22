package com.mysawit.shipment.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mysawit.shipment.domain.ShipmentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "shipments", indexes = {
    @jakarta.persistence.Index(name = "idx_shipments_supir_user_id", columnList = "supir_user_id"),
    @jakarta.persistence.Index(name = "idx_shipments_mandor_user_id", columnList = "mandor_user_id"),
    @jakarta.persistence.Index(name = "idx_shipments_status", columnList = "status"),
    @jakarta.persistence.Index(name = "idx_shipments_created_at", columnList = "created_at")
})
@Getter
@Setter
public class Shipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mandor_user_id", nullable = false)
    private UUID mandorUserId;

    @Column(name = "mandor_name", length = 100, columnDefinition = "TEXT")
    private String mandorName;

    @Column(name = "supir_user_id", nullable = false)
    private UUID supirUserId;

    @Column(name = "supir_name", length = 100, columnDefinition = "TEXT")
    private String supirName;

    @Column(name = "plantation_id", nullable = false, length = 64)
    private String plantationId;
    
    @Column(nullable = false)
    private String destination;
    
    @Column(name = "total_kg", nullable = false)
    private Double totalKg;

    @Column(name = "kg_accepted")
    private Double kgAccepted;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.MEMUAT;
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ShipmentItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "mandor_reviewed_at")
    private OffsetDateTime mandorReviewedAt;

    @Column(name = "admin_reviewed_at")
    private OffsetDateTime adminReviewedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void addItem(ShipmentItem item) {
        item.setShipment(this);
        items.add(item);
    }
}

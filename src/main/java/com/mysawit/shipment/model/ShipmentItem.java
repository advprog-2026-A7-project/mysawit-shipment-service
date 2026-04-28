package com.mysawit.shipment.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "shipment_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shipment_items_harvest_id",
                columnNames = "harvest_id"
        )
)
@Getter
@Setter
public class ShipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @NotNull(message = "Harvest ID is required")
    @Column(name = "harvest_id", nullable = false)
    private UUID harvestId;

    @NotNull(message = "Weight kg is required")
    @Positive(message = "Weight kg must be positive")
    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;
}

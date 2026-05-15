package com.mysawit.shipment.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "harvest_read_models")
@Getter
@Setter
public class HarvestReadModel {

    @Id
    @Column(name = "harvest_id", nullable = false, updatable = false)
    private UUID harvestId;

    @Column(name = "mandor_user_id", nullable = false)
    private UUID mandorUserId;

    @Column(name = "plantation_id", nullable = false, length = 64)
    private String plantationId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "last_event_id", length = 80)
    private String lastEventId;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

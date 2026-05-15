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
@Table(name = "worker_plantation_assignments")
@Getter
@Setter
public class WorkerPlantationAssignment {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "plantation_id", nullable = false, length = 64)
    private String plantationId;

    @Column(name = "last_event_id", length = 80)
    private String lastEventId;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

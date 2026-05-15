package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PlantationAssignmentEvent(
        String eventId,
        @JsonAlias({"userId", "mandorId", "supirId", "workerId"})
        UUID userId,
        @JsonAlias({"name", "username", "supirName", "mandorName"})
        String name,
        String role,
        @JsonAlias({"plantationId", "kebunId"})
        String plantationId,
        String action,
        OffsetDateTime occurredAt
) {
}

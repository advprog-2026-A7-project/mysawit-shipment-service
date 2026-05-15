package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public record HarvestShipmentEvent(
        String eventId,
        @JsonAlias({"harvestId", "id"})
        UUID harvestId,
        @JsonAlias({"mandorId", "mandorUserId", "foremanId"})
        UUID mandorUserId,
        @JsonAlias({"plantationId", "kebunId"})
        String plantationId,
        @JsonAlias({"weight", "weightKg"})
        Double weightKg,
        String status,
        OffsetDateTime occurredAt
) {
}

package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShipmentCompletedEvent(
        UUID shipmentId,
        UUID driverId,
        UUID mandorId,
        Double totalKg,
        List<UUID> harvestIds,
        OffsetDateTime completedAt
) {
}

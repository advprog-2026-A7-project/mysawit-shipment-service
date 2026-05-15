package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShipmentPayrollEvent(
        String eventId,
        UUID shipmentId,
        UUID employeeId,
        String employeeRole,
        Double kg,
        List<UUID> harvestIds,
        OffsetDateTime occurredAt
) {
}

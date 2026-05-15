package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationEvent(
        String eventId,
        UUID recipientId,
        String type,
        UUID referenceId,
        String message,
        OffsetDateTime occurredAt
) {
}

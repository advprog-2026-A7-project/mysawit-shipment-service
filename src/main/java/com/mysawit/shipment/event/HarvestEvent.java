package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HarvestEvent {
    private String eventId;
    private UUID harvestId;
    private UUID harvesterId;
    private UUID foremanId;
    private String plantationId;
    private Double weight;
    private String status;
    private OffsetDateTime occurredAt;
}

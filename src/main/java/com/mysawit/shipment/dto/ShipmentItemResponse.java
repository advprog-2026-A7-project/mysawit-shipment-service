package com.mysawit.shipment.dto;

import java.util.UUID;

import com.mysawit.shipment.model.ShipmentItem;

public record ShipmentItemResponse(
        UUID harvestId,
        Double weightKg
) {
    public static ShipmentItemResponse fromEntity(ShipmentItem item) {
        return new ShipmentItemResponse(
                item.getHarvestId(),
                item.getWeightKg()
        );
    }
}

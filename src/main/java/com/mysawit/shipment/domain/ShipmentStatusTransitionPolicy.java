package com.mysawit.shipment.domain;

import java.util.Map;

public final class ShipmentStatusTransitionPolicy {

    private static final Map<ShipmentStatus, ShipmentStatus> NEXT_STATUS = Map.of(
            ShipmentStatus.MEMUAT, ShipmentStatus.MENGIRIM,
            ShipmentStatus.MENGIRIM, ShipmentStatus.TIBA
    );

    private ShipmentStatusTransitionPolicy() {
    }

    public static boolean canTransition(ShipmentStatus from, ShipmentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return to == NEXT_STATUS.get(from);
    }
}

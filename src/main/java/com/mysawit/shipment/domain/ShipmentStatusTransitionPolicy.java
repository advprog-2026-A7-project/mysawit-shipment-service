package com.mysawit.shipment.domain;

public final class ShipmentStatusTransitionPolicy {

    private ShipmentStatusTransitionPolicy() {
    }

    public static boolean canTransition(ShipmentStatus from, ShipmentStatus to) {
        if (from == ShipmentStatus.MEMUAT) {
            return to == ShipmentStatus.MENGIRIM;
        }
        if (from == ShipmentStatus.MENGIRIM) {
            return to == ShipmentStatus.TIBA;
        }
        return false;
    }
}

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
        return canDriverTransition(from, to)
                || canMandorDecision(from, to)
                || canAdminDecision(from, to);
    }

    public static boolean canDriverTransition(ShipmentStatus from, ShipmentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return to == NEXT_STATUS.get(from);
    }

    public static boolean canMandorDecision(ShipmentStatus from, ShipmentStatus to) {
        return from == ShipmentStatus.TIBA
                && (to == ShipmentStatus.MANDOR_APPROVED || to == ShipmentStatus.MANDOR_REJECTED);
    }

    public static boolean canAdminDecision(ShipmentStatus from, ShipmentStatus to) {
        return from == ShipmentStatus.MANDOR_APPROVED
                && (to == ShipmentStatus.ADMIN_APPROVED
                        || to == ShipmentStatus.ADMIN_REJECTED
                        || to == ShipmentStatus.PARTIALLY_REJECTED);
    }
}

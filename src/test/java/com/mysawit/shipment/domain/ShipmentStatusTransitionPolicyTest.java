package com.mysawit.shipment.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipmentStatusTransitionPolicyTest {

    @Test
    void allowsTransitionFromMemuatToMengirim() {
        assertTrue(ShipmentStatusTransitionPolicy.canTransition(ShipmentStatus.MEMUAT, ShipmentStatus.MENGIRIM));
    }

    @Test
    void allowsTransitionFromMengirimToTiba() {
        assertTrue(ShipmentStatusTransitionPolicy.canTransition(ShipmentStatus.MENGIRIM, ShipmentStatus.TIBA));
    }

    @Test
    void rejectsSkippingFromMemuatToTiba() {
        assertFalse(ShipmentStatusTransitionPolicy.canTransition(ShipmentStatus.MEMUAT, ShipmentStatus.TIBA));
    }

    @Test
    void rejectsAnyTransitionFromTerminalTiba() {
        assertFalse(ShipmentStatusTransitionPolicy.canTransition(ShipmentStatus.TIBA, ShipmentStatus.MENGIRIM));
    }
}

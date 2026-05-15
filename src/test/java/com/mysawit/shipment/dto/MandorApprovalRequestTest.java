package com.mysawit.shipment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class MandorApprovalRequestTest {

    @Test
    void recordStoresStatusAndReason() {
        MandorApprovalRequest request = new MandorApprovalRequest("MANDOR_REJECTED", "bad seal");

        assertEquals("MANDOR_REJECTED", request.status());
        assertEquals("bad seal", request.rejectionReason());
    }
}

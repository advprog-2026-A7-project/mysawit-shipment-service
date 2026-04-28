package com.mysawit.shipment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AdminApprovalRequestTest {

    @Test
    void recordStoresStatusValue() {
        AdminApprovalRequest request = new AdminApprovalRequest("ADMIN_APPROVED");

        assertEquals("ADMIN_APPROVED", request.status());
    }
}

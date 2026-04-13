package com.mysawit.shipment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class UpdateStatusRequestTest {

    @Test
    void recordStoresStatusValue() {
        UpdateStatusRequest request = new UpdateStatusRequest("MENGIRIM");

        assertEquals("MENGIRIM", request.status());
    }

    @Test
    void recordAllowsNullStatus() {
        UpdateStatusRequest request = new UpdateStatusRequest(null);

        assertNull(request.status());
    }
}

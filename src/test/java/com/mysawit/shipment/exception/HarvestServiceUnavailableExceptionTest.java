package com.mysawit.shipment.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class HarvestServiceUnavailableExceptionTest {

    @Test
    void constructorStoresCause() {
        RuntimeException cause = new RuntimeException("down");

        HarvestServiceUnavailableException exception =
                new HarvestServiceUnavailableException("Harvest service is unavailable", cause);

        assertEquals("Harvest service is unavailable", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}

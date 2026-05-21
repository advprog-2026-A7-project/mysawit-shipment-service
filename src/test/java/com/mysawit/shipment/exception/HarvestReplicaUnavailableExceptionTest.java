package com.mysawit.shipment.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class HarvestReplicaUnavailableExceptionTest {

    @Test
    void constructorStoresCause() {
        RuntimeException cause = new RuntimeException("down");

        HarvestReplicaUnavailableException exception =
                new HarvestReplicaUnavailableException("Harvest data not yet replicated", cause);

        assertEquals("Harvest data not yet replicated", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}

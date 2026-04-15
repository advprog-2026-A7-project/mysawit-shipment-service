package com.mysawit.shipment.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HarvestValidationExceptionTest {

    @Test
    void constructorDefaultsToBadRequestStatus() {
        HarvestValidationException exception = new HarvestValidationException("invalid harvest");

        assertEquals("invalid harvest", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}

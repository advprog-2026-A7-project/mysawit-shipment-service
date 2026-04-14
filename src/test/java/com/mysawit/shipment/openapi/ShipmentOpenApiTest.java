package com.mysawit.shipment.openapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ShipmentOpenApiTest {

    private static final Path SPEC_PATH = Path.of("src/main/resources/openapi/shipment-api.yaml");

    @Test
    void createShipmentDocumentsHarvestValidationAndServiceUnavailableResponses() throws IOException {
        String spec = Files.readString(SPEC_PATH);

        assertContains(spec, "HARVEST_VALIDATION_FAILED");
        assertContains(spec, "HARVEST_SERVICE_UNAVAILABLE");
        assertContains(spec, "\"503\":");
    }

    private void assertContains(String spec, String expectedValue) {
        assertTrue(spec.contains(expectedValue));
    }
}

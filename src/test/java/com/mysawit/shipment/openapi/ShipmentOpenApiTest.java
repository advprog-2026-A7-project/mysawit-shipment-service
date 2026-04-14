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

        assertTrue(spec.contains("HARVEST_VALIDATION_FAILED"));
        assertTrue(spec.contains("HARVEST_SERVICE_UNAVAILABLE"));
        assertTrue(spec.contains("\"503\":"));
    }
}

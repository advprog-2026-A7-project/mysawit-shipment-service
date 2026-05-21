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
        assertContains(spec, "HARVEST_REPLICA_UNAVAILABLE");
        assertContains(spec, "\"404\":");
        assertContains(spec, "\"409\":");
        assertContains(spec, "Harvest already claimed:");
        assertContains(spec, "\"503\":");
    }

    @Test
    void adminApprovalEndpointDocumentsDecisionsAndRequiredAdminRole() throws IOException {
        String spec = Files.readString(SPEC_PATH);

        assertContains(spec, "/api/shipments/{id}/admin-approval:");
        assertContains(spec, "ADMIN_APPROVED");
        assertContains(spec, "PARTIALLY_REJECTED");
        assertContains(spec, "Admin approval");
    }

    private void assertContains(String spec, String expectedValue) {
        assertTrue(spec.contains(expectedValue));
    }
}

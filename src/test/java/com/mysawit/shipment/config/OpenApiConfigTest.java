package com.mysawit.shipment.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void customOpenAPIBeanIsCreated() {
        OpenAPI api = openApiConfig.customOpenAPI();

        assertNotNull(api);
    }

    @Test
    void customOpenAPIHasBearerAuthSecurityScheme() {
        OpenAPI api = openApiConfig.customOpenAPI();

        assertNotNull(api.getComponents());
        assertNotNull(api.getComponents().getSecuritySchemes());
        assertNotNull(api.getComponents().getSecuritySchemes().get("bearerAuth"));
    }

    @Test
    void customOpenAPIHasSecurityRequirement() {
        OpenAPI api = openApiConfig.customOpenAPI();

        assertNotNull(api.getSecurity());
        assertNotNull(api.getSecurity().get(0).get("bearerAuth"));
    }
}

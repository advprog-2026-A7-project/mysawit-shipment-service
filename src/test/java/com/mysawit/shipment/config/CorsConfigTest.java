package com.mysawit.shipment.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void corsConfigurationSourceBuildsExpectedConfiguration() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:3000,http://localhost:5173");

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;
        CorsConfiguration configuration = urlSource.getCorsConfigurations().get("/**");

        assertNotNull(configuration);
        assertEquals(2, configuration.getAllowedOrigins().size());
        assertTrue(configuration.getAllowedMethods().contains("GET"));
        assertEquals(1, configuration.getAllowedHeaders().size());
        assertEquals("*", configuration.getAllowedHeaders().get(0));
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }
}

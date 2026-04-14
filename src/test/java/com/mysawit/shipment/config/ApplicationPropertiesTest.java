package com.mysawit.shipment.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ApplicationPropertiesTest {

    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String RABBIT_HEALTH_PROPERTY = "management.health.rabbit.enabled";

    @Test
    void applicationPropertiesDisablesRabbitHealthIndicator() throws IOException {
        Properties properties = loadApplicationProperties();

        assertEquals("false", properties.getProperty(RABBIT_HEALTH_PROPERTY));
    }

    private Properties loadApplicationProperties() throws IOException {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(APPLICATION_PROPERTIES)) {
            assertNotNull(inputStream);

            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        }
    }
}

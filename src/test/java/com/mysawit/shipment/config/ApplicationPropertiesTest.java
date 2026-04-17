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
    private static final String READINESS_GROUP_PROPERTY =
            "management.endpoint.health.group.readiness.include";

    @Test
    void applicationPropertiesDisablesRabbitHealthIndicator() throws IOException {
        Properties properties = loadApplicationProperties();

        assertEquals("false", properties.getProperty(RABBIT_HEALTH_PROPERTY));
    }

    @Test
    void applicationPropertiesIncludeDatabaseInReadinessHealth() throws IOException {
        Properties properties = loadApplicationProperties();

        assertEquals("readinessState,db", properties.getProperty(READINESS_GROUP_PROPERTY));
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

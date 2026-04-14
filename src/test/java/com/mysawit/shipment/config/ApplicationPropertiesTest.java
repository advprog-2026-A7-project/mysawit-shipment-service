package com.mysawit.shipment.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ApplicationPropertiesTest {

    @Test
    void applicationPropertiesDisablesRabbitHealthIndicator() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(inputStream);

            Properties properties = new Properties();
            properties.load(inputStream);

            assertEquals("false", properties.getProperty("management.health.rabbit.enabled"));
        }
    }
}

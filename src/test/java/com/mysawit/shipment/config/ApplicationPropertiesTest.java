package com.mysawit.shipment.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ApplicationPropertiesTest {

    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String HIKARI_MAX_POOL_SIZE_PROPERTY =
            "spring.datasource.hikari.maximum-pool-size";
    private static final String HIKARI_MIN_IDLE_PROPERTY =
            "spring.datasource.hikari.minimum-idle";
    private static final String HIKARI_CONNECTION_TIMEOUT_PROPERTY =
            "spring.datasource.hikari.connection-timeout";
    private static final String RABBIT_HEALTH_PROPERTY = "management.health.rabbit.enabled";
    private static final String ENDPOINT_EXPOSURE_PROPERTY =
            "management.endpoints.web.exposure.include";
    private static final String APPLICATION_METRICS_TAG_PROPERTY =
            "management.metrics.tags.application";
    private static final String HTTP_REQUEST_HISTOGRAM_PROPERTY =
            "management.metrics.distribution.percentiles-histogram.http.server.requests";
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

    @Test
    void applicationPropertiesLimitDatabaseConnectionPool() throws IOException {
        Properties properties = loadApplicationProperties();

        assertEquals("${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:3}",
                properties.getProperty(HIKARI_MAX_POOL_SIZE_PROPERTY));
        assertEquals("${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE:1}",
                properties.getProperty(HIKARI_MIN_IDLE_PROPERTY));
        assertEquals("${SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT:5000}",
                properties.getProperty(HIKARI_CONNECTION_TIMEOUT_PROPERTY));
    }

    @Test
    void applicationPropertiesExposePrometheusMetrics() throws IOException {
        Properties properties = loadApplicationProperties();

        assertEquals("health,info,metrics,prometheus",
                properties.getProperty(ENDPOINT_EXPOSURE_PROPERTY));
    }

    @Test
    void applicationPropertiesTagMetricsWithApplicationName() throws IOException {
        Properties properties = loadApplicationProperties();

        assertEquals("${spring.application.name}",
                properties.getProperty(APPLICATION_METRICS_TAG_PROPERTY));
    }

    @Test
    void applicationPropertiesEnableHttpRequestHistogram() throws IOException {
        Properties properties = loadApplicationProperties();

        assertEquals("true", properties.getProperty(HTTP_REQUEST_HISTOGRAM_PROPERTY));
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

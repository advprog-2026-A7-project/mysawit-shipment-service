package com.mysawit.shipment.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;

import com.mysawit.shipment.event.HarvestEventConsumer;
import com.mysawit.shipment.event.PlantationAssignmentEventListener;
import com.mysawit.shipment.event.UserAssignmentEventConsumer;
import com.mysawit.shipment.event.UserDeletedEventConsumer;
import com.mysawit.shipment.event.UserRegisteredEventConsumer;
import com.mysawit.shipment.event.UserUpdatedEventConsumer;

/**
 * Regression guard: every {@link RabbitListener} queue name in the shipment service must resolve
 * to a queue that {@link RabbitMqConfig} actually declares as a {@code @Bean Queue}. This catches
 * the class of bug where the Java constant on a {@code @Bean Queue} drifts apart from the property
 * placeholder used in {@code @RabbitListener(queues = "${...}")}: the queue ends up bound to the
 * exchange but the listener polls a different (unbound) queue, so messages pile up forever.
 */
class RabbitMqQueueBindingsConsistencyTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?\\}");

    private static final Class<?>[] LISTENER_BEARING_CLASSES = {
            HarvestEventConsumer.class,
            PlantationAssignmentEventListener.class,
            UserRegisteredEventConsumer.class,
            UserAssignmentEventConsumer.class,
            UserUpdatedEventConsumer.class,
            UserDeletedEventConsumer.class
    };

    @Test
    void everyRabbitListenerQueueIsDeclaredAsBeanInRabbitMqConfig() throws Exception {
        Set<String> declaredQueueNames = collectDeclaredQueueNames();
        Properties applicationProperties = loadApplicationProperties();

        Set<String> listenerQueues = new LinkedHashSet<>();
        for (Class<?> listenerClass : LISTENER_BEARING_CLASSES) {
            for (Method method : listenerClass.getDeclaredMethods()) {
                RabbitListener listener = method.getAnnotation(RabbitListener.class);
                if (listener == null) {
                    continue;
                }
                String[] queues = listener.queues();
                assertNotNull(queues, listenerClass.getSimpleName() + "#" + method.getName()
                        + ": @RabbitListener(queues = ...) must not be null");
                assertEquals(1, queues.length, listenerClass.getSimpleName() + "#" + method.getName()
                        + ": expected exactly one queue per listener");

                String resolved = resolvePlaceholder(queues[0], applicationProperties);
                listenerQueues.add(resolved);

                assertTrue(declaredQueueNames.contains(resolved),
                        () -> "@RabbitListener on " + listenerClass.getSimpleName() + "#" + method.getName()
                                + " resolves to '" + resolved + "', which is not declared as a @Bean Queue in "
                                + "RabbitMqConfig. Declared queues: " + declaredQueueNames);
            }
        }

        // Every listener-bearing class above is expected to have at least one @RabbitListener.
        assertEquals(LISTENER_BEARING_CLASSES.length, listenerQueues.size(),
                "Expected one resolved listener queue per consumer class, got: " + listenerQueues);
    }

    @Test
    void propertyPlaceholderDefaultsMatchTheConfiguredPropertyValues() throws IOException {
        Properties applicationProperties = loadApplicationProperties();

        for (Class<?> listenerClass : LISTENER_BEARING_CLASSES) {
            for (Method method : listenerClass.getDeclaredMethods()) {
                RabbitListener listener = method.getAnnotation(RabbitListener.class);
                if (listener == null) {
                    continue;
                }
                String rawQueue = listener.queues()[0];
                Matcher matcher = PLACEHOLDER.matcher(rawQueue);
                if (!matcher.matches()) {
                    continue;
                }

                String propertyKey = matcher.group(1);
                String defaultValue = matcher.group(2);
                String configuredValue = applicationProperties.getProperty(propertyKey);

                assertNotNull(configuredValue,
                        "Property '" + propertyKey + "' referenced by @RabbitListener on "
                                + listenerClass.getSimpleName() + "#" + method.getName()
                                + " is missing from application.properties");
                assertFalse(defaultValue == null || defaultValue.isBlank(),
                        "@RabbitListener on " + listenerClass.getSimpleName() + "#" + method.getName()
                                + " is missing a default for placeholder '" + propertyKey + "'");
                assertEquals(configuredValue, defaultValue,
                        "@RabbitListener default for '" + propertyKey + "' on "
                                + listenerClass.getSimpleName() + "#" + method.getName()
                                + " disagrees with application.properties value");
            }
        }
    }

    private Set<String> collectDeclaredQueueNames() throws Exception {
        RabbitMqConfig config = new RabbitMqConfig();
        Set<String> queueNames = new HashSet<>();
        for (Method method : RabbitMqConfig.class.getDeclaredMethods()) {
            if (method.getAnnotation(Bean.class) == null) {
                continue;
            }
            if (!Queue.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                continue;
            }
            Queue queue = (Queue) method.invoke(config);
            queueNames.add(queue.getName());
        }
        assertFalse(queueNames.isEmpty(), "RabbitMqConfig must declare at least one @Bean Queue");
        return queueNames;
    }

    private String resolvePlaceholder(String value, Properties properties) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        String key = matcher.group(1);
        String defaultValue = matcher.group(2);
        String resolved = properties.getProperty(key);
        if (resolved != null && !resolved.isBlank()) {
            return resolved;
        }
        assertNotNull(defaultValue,
                "Property placeholder '" + value + "' has no value and no default");
        return defaultValue;
    }

    private Properties loadApplicationProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            assertNotNull(inputStream, "application.properties is missing from the classpath");
            properties.load(inputStream);
        }
        return properties;
    }
}

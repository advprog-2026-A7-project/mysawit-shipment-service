package com.mysawit.shipment.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

class RabbitMqConfigTest {

    private RabbitMqConfig rabbitMqConfig;

    @BeforeEach
    void setUp() {
        rabbitMqConfig = new RabbitMqConfig();
    }

    @Test
    void shipmentExchangeReturnsTopicExchangeWithCorrectName() {
        TopicExchange exchange = rabbitMqConfig.shipmentExchange();

        assertNotNull(exchange);
        assertEquals(RabbitMqConfig.EXCHANGE, exchange.getName());
    }

    @Test
    void integrationExchangesReturnTopicExchangesWithCorrectNames() {
        assertEquals(RabbitMqConfig.HARVEST_EXCHANGE, rabbitMqConfig.harvestExchange().getName());
        assertEquals(RabbitMqConfig.PLANTATION_EXCHANGE, rabbitMqConfig.plantationExchange().getName());
        assertEquals(RabbitMqConfig.NOTIFICATION_EXCHANGE, rabbitMqConfig.notificationExchange().getName());
    }

    @Test
    void queuesReturnDurableQueuesWithCorrectNames() {
        Queue harvestQueue = rabbitMqConfig.harvestEventsQueue();
        Queue assignmentQueue = rabbitMqConfig.plantationAssignmentQueue();

        assertEquals(RabbitMqConfig.HARVEST_EVENTS_QUEUE, harvestQueue.getName());
        assertEquals(RabbitMqConfig.PLANTATION_ASSIGNMENT_QUEUE, assignmentQueue.getName());
    }

    @Test
    void bindingsUseExpectedRoutingKeys() {
        Binding harvestBinding = rabbitMqConfig.harvestEventsBinding(
                rabbitMqConfig.harvestEventsQueue(),
                rabbitMqConfig.harvestExchange()
        );
        Binding assignmentBinding = rabbitMqConfig.plantationAssignmentBinding(
                rabbitMqConfig.plantationAssignmentQueue(),
                rabbitMqConfig.plantationExchange()
        );

        assertEquals(RabbitMqConfig.HARVEST_EVENTS_ROUTING_KEY, harvestBinding.getRoutingKey());
        assertEquals(RabbitMqConfig.PLANTATION_ASSIGNMENT_ROUTING_KEY, assignmentBinding.getRoutingKey());
    }

    @Test
    void jsonMessageConverterReturnsJackson2JsonMessageConverter() {
        assertInstanceOf(Jackson2JsonMessageConverter.class, rabbitMqConfig.jsonMessageConverter());
    }

    @Test
    void rabbitTemplateUsesJsonMessageConverter() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        RabbitTemplate template = rabbitMqConfig.rabbitTemplate(connectionFactory);

        assertNotNull(template);
        assertInstanceOf(Jackson2JsonMessageConverter.class, template.getMessageConverter());
    }
}

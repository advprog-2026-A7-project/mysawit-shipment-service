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
        assertEquals(RabbitMqConfig.USER_EXCHANGE, rabbitMqConfig.userExchange().getName());
        assertEquals(RabbitMqConfig.NOTIFICATION_EXCHANGE, rabbitMqConfig.notificationExchange().getName());
    }

    @Test
    void queuesReturnDurableQueuesWithCorrectNames() {
        Queue harvestQueue = rabbitMqConfig.harvestEventsQueue();
        Queue assignmentQueue = rabbitMqConfig.plantationAssignmentQueue();
        Queue userRegisteredQueue = rabbitMqConfig.userRegisteredQueue();
        Queue userAssignmentQueue = rabbitMqConfig.userAssignmentQueue();
        Queue userUpdatedQueue = rabbitMqConfig.userUpdatedQueue();
        Queue userDeletedQueue = rabbitMqConfig.userDeletedQueue();

        assertEquals(RabbitMqConfig.HARVEST_EVENTS_QUEUE, harvestQueue.getName());
        assertEquals(RabbitMqConfig.PLANTATION_ASSIGNMENT_QUEUE, assignmentQueue.getName());
        assertEquals(RabbitMqConfig.USER_REGISTERED_QUEUE, userRegisteredQueue.getName());
        assertEquals(RabbitMqConfig.USER_ASSIGNMENT_QUEUE, userAssignmentQueue.getName());
        assertEquals(RabbitMqConfig.USER_UPDATED_QUEUE, userUpdatedQueue.getName());
        assertEquals(RabbitMqConfig.USER_DELETED_QUEUE, userDeletedQueue.getName());
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
        Binding userRegisteredBinding = rabbitMqConfig.userRegisteredBinding(
                rabbitMqConfig.userRegisteredQueue(),
                rabbitMqConfig.userExchange()
        );
        Binding userAssignmentBinding = rabbitMqConfig.userAssignmentBinding(
                rabbitMqConfig.userAssignmentQueue(),
                rabbitMqConfig.userExchange()
        );
        Binding userUpdatedBinding = rabbitMqConfig.userUpdatedBinding(
                rabbitMqConfig.userUpdatedQueue(),
                rabbitMqConfig.userExchange()
        );
        Binding userDeletedBinding = rabbitMqConfig.userDeletedBinding(
                rabbitMqConfig.userDeletedQueue(),
                rabbitMqConfig.userExchange()
        );

        assertEquals(RabbitMqConfig.HARVEST_EVENTS_ROUTING_KEY, harvestBinding.getRoutingKey());
        assertEquals(RabbitMqConfig.PLANTATION_ASSIGNMENT_ROUTING_KEY, assignmentBinding.getRoutingKey());
        assertEquals(RabbitMqConfig.USER_REGISTERED_ROUTING_KEY, userRegisteredBinding.getRoutingKey());
        assertEquals(RabbitMqConfig.USER_ASSIGNMENT_ROUTING_KEY, userAssignmentBinding.getRoutingKey());
        assertEquals(RabbitMqConfig.USER_UPDATED_ROUTING_KEY, userUpdatedBinding.getRoutingKey());
        assertEquals(RabbitMqConfig.USER_DELETED_ROUTING_KEY, userDeletedBinding.getRoutingKey());
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

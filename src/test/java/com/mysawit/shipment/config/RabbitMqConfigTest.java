package com.mysawit.shipment.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

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

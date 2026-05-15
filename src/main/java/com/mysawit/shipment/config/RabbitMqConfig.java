package com.mysawit.shipment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "shipment.exchange";
    public static final String HARVEST_EXCHANGE = "harvest.exchange";
    public static final String PLANTATION_EXCHANGE = "plantation.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String HARVEST_EVENTS_QUEUE = "shipment.harvest-events.queue";
    public static final String PLANTATION_ASSIGNMENT_QUEUE = "shipment.plantation-assignment.queue";
    public static final String HARVEST_EVENTS_ROUTING_KEY = "harvest.*";
    public static final String PLANTATION_ASSIGNMENT_ROUTING_KEY = "plantation.assignment.*";

    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange harvestExchange() {
        return new TopicExchange(HARVEST_EXCHANGE);
    }

    @Bean
    public TopicExchange plantationExchange() {
        return new TopicExchange(PLANTATION_EXCHANGE);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue harvestEventsQueue() {
        return new Queue(HARVEST_EVENTS_QUEUE, true);
    }

    @Bean
    public Queue plantationAssignmentQueue() {
        return new Queue(PLANTATION_ASSIGNMENT_QUEUE, true);
    }

    @Bean
    public Binding harvestEventsBinding(
            @Qualifier("harvestEventsQueue") Queue harvestEventsQueue,
            @Qualifier("harvestExchange") TopicExchange harvestExchange
    ) {
        return BindingBuilder.bind(harvestEventsQueue).to(harvestExchange).with(HARVEST_EVENTS_ROUTING_KEY);
    }

    @Bean
    public Binding plantationAssignmentBinding(
            @Qualifier("plantationAssignmentQueue") Queue plantationAssignmentQueue,
            @Qualifier("plantationExchange") TopicExchange plantationExchange
    ) {
        return BindingBuilder.bind(plantationAssignmentQueue).to(plantationExchange).with(PLANTATION_ASSIGNMENT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

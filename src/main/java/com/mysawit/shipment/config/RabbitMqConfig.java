package com.mysawit.shipment.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mysawit.shipment.event.HarvestEvent;
import com.mysawit.shipment.event.PlantationAssignmentEvent;
import com.mysawit.shipment.event.UserAssignmentEvent;
import com.mysawit.shipment.event.UserDeletedEvent;
import com.mysawit.shipment.event.UserRegisteredEvent;
import com.mysawit.shipment.event.UserUpdatedEvent;

@Configuration
public class RabbitMqConfig {

    private static final String USER_EXCHANGE_BEAN = "userExchange";

    public static final String EXCHANGE = "shipment.exchange";
    public static final String HARVEST_EXCHANGE = "harvest.exchange";
    public static final String PLANTATION_EXCHANGE = "plantation.exchange";
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String HARVEST_EVENTS_QUEUE = "shipment.harvest-events.queue";
    public static final String PLANTATION_ASSIGNMENT_QUEUE = "shipment.plantation-assignment.queue";
    public static final String USER_REGISTERED_QUEUE = "shipment.user.registered.queue";
    public static final String USER_ASSIGNMENT_QUEUE = "shipment.user.assignment.queue";
    public static final String USER_UPDATED_QUEUE = "shipment.user.updated.queue";
    public static final String USER_DELETED_QUEUE = "shipment.user.deleted.queue";
    public static final String HARVEST_EVENTS_ROUTING_KEY = "harvest.*";
    public static final String PLANTATION_ASSIGNMENT_ROUTING_KEY = "plantation.assignment.*";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_ASSIGNMENT_ROUTING_KEY = "user.assignment.*";
    public static final String USER_UPDATED_ROUTING_KEY = "user.updated";
    public static final String USER_DELETED_ROUTING_KEY = "user.deleted";

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
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
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
    public Queue userRegisteredQueue() {
        return new Queue(USER_REGISTERED_QUEUE, true);
    }

    @Bean
    public Queue userAssignmentQueue() {
        return new Queue(USER_ASSIGNMENT_QUEUE, true);
    }

    @Bean
    public Queue userUpdatedQueue() {
        return new Queue(USER_UPDATED_QUEUE, true);
    }

    @Bean
    public Queue userDeletedQueue() {
        return new Queue(USER_DELETED_QUEUE, true);
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
    public Binding userRegisteredBinding(
            @Qualifier("userRegisteredQueue") Queue userRegisteredQueue,
            @Qualifier(USER_EXCHANGE_BEAN) TopicExchange userExchange
    ) {
        return BindingBuilder.bind(userRegisteredQueue).to(userExchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding userAssignmentBinding(
            @Qualifier("userAssignmentQueue") Queue userAssignmentQueue,
            @Qualifier(USER_EXCHANGE_BEAN) TopicExchange userExchange
    ) {
        return BindingBuilder.bind(userAssignmentQueue).to(userExchange).with(USER_ASSIGNMENT_ROUTING_KEY);
    }

    @Bean
    public Binding userUpdatedBinding(
            @Qualifier("userUpdatedQueue") Queue userUpdatedQueue,
            @Qualifier(USER_EXCHANGE_BEAN) TopicExchange userExchange
    ) {
        return BindingBuilder.bind(userUpdatedQueue).to(userExchange).with(USER_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding userDeletedBinding(
            @Qualifier("userDeletedQueue") Queue userDeletedQueue,
            @Qualifier(USER_EXCHANGE_BEAN) TopicExchange userExchange
    ) {
        return BindingBuilder.bind(userDeletedQueue).to(userExchange).with(USER_DELETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("*");

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        // Harvest service publishes HarvestPayrollEvent on the harvest.exchange (routing key
        // harvest.approved). The shipment service consumes it as HarvestEvent.
        idClassMapping.put("com.mysawit.harvest.event.HarvestPayrollEvent", HarvestEvent.class);
        // Identity service publishes user lifecycle events on the user.exchange.
        idClassMapping.put("com.mysawit.identity.event.UserRegisteredEvent", UserRegisteredEvent.class);
        idClassMapping.put("com.mysawit.identity.event.UserDeletedEvent", UserDeletedEvent.class);
        // Identity service uses UserAssignedEvent for BURUH<->MANDOR assignment events; the
        // shipment service models the same payload as UserAssignmentEvent.
        idClassMapping.put("com.mysawit.identity.event.UserAssignedEvent", UserAssignmentEvent.class);
        idClassMapping.put("com.mysawit.identity.event.UserAssignmentEvent", UserAssignmentEvent.class);
        // No producer of a UserUpdatedEvent has been located in identity-service; the mapping is
        // declared defensively in case a future producer publishes one with this type id.
        idClassMapping.put("com.mysawit.identity.event.UserUpdatedEvent", UserUpdatedEvent.class);
        // Plantation service publishes plantation assignment events as a HashMap (no __TypeId__),
        // but the mapping is declared so a future strongly-typed publisher continues to deserialize.
        idClassMapping.put("com.mysawit.plantation.event.PlantationAssignmentEvent", PlantationAssignmentEvent.class);
        classMapper.setIdClassMapping(idClassMapping);

        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

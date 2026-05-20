package com.mysawit.shipment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mysawit.shipment.service.UserReplicaService;

@Component
public class UserRegisteredEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventConsumer.class);

    private final UserReplicaService userReplicaService;

    public UserRegisteredEventConsumer(UserReplicaService userReplicaService) {
        this.userReplicaService = userReplicaService;
    }

    @RabbitListener(queues = "${shipment.rabbitmq.queues.user-registered:shipment.user.registered.queue}")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("Received user.registered userId={}", event.getUserId());
        userReplicaService.upsertFromRegistration(event);
    }
}

package com.mysawit.shipment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mysawit.shipment.service.UserReplicaService;

@Component
public class UserUpdatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserUpdatedEventConsumer.class);

    private final UserReplicaService userReplicaService;

    public UserUpdatedEventConsumer(UserReplicaService userReplicaService) {
        this.userReplicaService = userReplicaService;
    }

    @RabbitListener(queues = "${shipment.rabbitmq.queues.user-updated:shipment.user.updated.queue}")
    public void onUserUpdated(UserUpdatedEvent event) {
        log.debug("Received user.updated userId={}", event.getUserId());
        userReplicaService.upsertFromUpdate(event);
    }
}

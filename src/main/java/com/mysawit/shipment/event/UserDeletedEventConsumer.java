package com.mysawit.shipment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mysawit.shipment.service.UserReplicaService;

@Component
public class UserDeletedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedEventConsumer.class);

    private final UserReplicaService userReplicaService;

    public UserDeletedEventConsumer(UserReplicaService userReplicaService) {
        this.userReplicaService = userReplicaService;
    }

    @RabbitListener(queues = "${shipment.rabbitmq.queues.user-deleted:shipment.user.deleted.queue}")
    public void onUserDeleted(UserDeletedEvent event) {
        log.debug("Received user.deleted userId={}", event.getUserId());
        userReplicaService.markDeleted(event);
    }
}

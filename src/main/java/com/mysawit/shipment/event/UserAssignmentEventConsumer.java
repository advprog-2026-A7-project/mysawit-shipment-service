package com.mysawit.shipment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mysawit.shipment.service.UserReplicaService;

@Component
public class UserAssignmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserAssignmentEventConsumer.class);

    private final UserReplicaService userReplicaService;

    public UserAssignmentEventConsumer(UserReplicaService userReplicaService) {
        this.userReplicaService = userReplicaService;
    }

    @RabbitListener(queues = "${shipment.rabbitmq.queues.user-assignment:shipment.user.assignment.queue}")
    public void onUserAssignment(UserAssignmentEvent event) {
        log.debug("Received user.assignment userId={} action={}", event.getUserId(), event.getAction());
        userReplicaService.applyAssignment(event);
    }
}

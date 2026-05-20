package com.mysawit.shipment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mysawit.shipment.service.PlantationAssignmentReplicaService;

@Component
public class PlantationAssignmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlantationAssignmentEventConsumer.class);

    private final PlantationAssignmentReplicaService plantationAssignmentReplicaService;

    public PlantationAssignmentEventConsumer(PlantationAssignmentReplicaService plantationAssignmentReplicaService) {
        this.plantationAssignmentReplicaService = plantationAssignmentReplicaService;
    }

    @RabbitListener(queues = "${shipment.rabbitmq.queues.plantation-assignment:shipment.plantation.assignment.queue}")
    public void onPlantationAssignment(PlantationAssignmentEvent event) {
        log.debug(
                "Received plantation.assignment userId={} role={} action={}",
                event.getUserId(),
                event.getRole(),
                event.getAction()
        );
        plantationAssignmentReplicaService.applyAssignment(event);
    }
}

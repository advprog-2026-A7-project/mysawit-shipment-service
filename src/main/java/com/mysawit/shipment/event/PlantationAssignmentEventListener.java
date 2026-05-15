package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mysawit.shipment.config.RabbitMqConfig;
import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;

@Component
public class PlantationAssignmentEventListener {

    private static final String ACTION_UNASSIGNED = "UNASSIGNED";

    private final WorkerPlantationAssignmentRepository workerPlantationAssignmentRepository;

    public PlantationAssignmentEventListener(WorkerPlantationAssignmentRepository workerPlantationAssignmentRepository) {
        this.workerPlantationAssignmentRepository = workerPlantationAssignmentRepository;
    }

    @Transactional
    @RabbitListener(queues = RabbitMqConfig.PLANTATION_ASSIGNMENT_QUEUE)
    public void handleAssignmentEvent(PlantationAssignmentEvent event) {
        if (!hasIdentity(event)) {
            return;
        }
        if (isUnassign(event)) {
            workerPlantationAssignmentRepository.deleteById(event.userId());
            return;
        }
        if (!hasAssignmentTarget(event)) {
            return;
        }

        WorkerPlantationAssignment assignment = workerPlantationAssignmentRepository.findById(event.userId())
                .orElseGet(WorkerPlantationAssignment::new);
        assignment.setUserId(event.userId());
        assignment.setRole(event.role().trim().toUpperCase(Locale.ROOT));
        assignment.setName(trimToNull(event.name()));
        assignment.setPlantationId(event.plantationId().trim());
        assignment.setLastEventId(event.eventId());
        assignment.setUpdatedAt(resolveOccurredAt(event));

        workerPlantationAssignmentRepository.save(assignment);
    }

    private boolean hasIdentity(PlantationAssignmentEvent event) {
        return event != null && event.userId() != null && event.role() != null && !event.role().isBlank();
    }

    private boolean isUnassign(PlantationAssignmentEvent event) {
        return event.action() != null && ACTION_UNASSIGNED.equalsIgnoreCase(event.action().trim());
    }

    private boolean hasAssignmentTarget(PlantationAssignmentEvent event) {
        return event.plantationId() != null && !event.plantationId().isBlank();
    }

    private OffsetDateTime resolveOccurredAt(PlantationAssignmentEvent event) {
        return event.occurredAt() != null ? event.occurredAt() : OffsetDateTime.now();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

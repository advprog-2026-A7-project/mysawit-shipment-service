package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mysawit.shipment.config.RabbitMqConfig;
import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;

@Component
public class PlantationAssignmentEventListener {

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
            workerPlantationAssignmentRepository.deleteById(parseUuid(event.getUserId()));
            return;
        }
        if (!hasAssignmentTarget(event)) {
            return;
        }

        UUID userId = parseUuid(event.getUserId());
        WorkerPlantationAssignment assignment = workerPlantationAssignmentRepository.findById(userId)
                .orElseGet(WorkerPlantationAssignment::new);
        assignment.setUserId(userId);
        assignment.setRole(event.getRole().trim().toUpperCase(Locale.ROOT));
        assignment.setName(trimToNull(event.getName()));
        assignment.setPlantationId(event.getPlantationId().trim());
        assignment.setLastEventId(event.getEventId());
        assignment.setUpdatedAt(resolveOccurredAt(event));

        workerPlantationAssignmentRepository.save(assignment);
    }

    private boolean hasIdentity(PlantationAssignmentEvent event) {
        return event != null && event.getUserId() != null && event.getRole() != null && !event.getRole().isBlank();
    }

    private boolean isUnassign(PlantationAssignmentEvent event) {
        return PlantationAssignmentEvent.AssignmentAction.UNASSIGNED.equals(event.getAction());
    }

    private boolean hasAssignmentTarget(PlantationAssignmentEvent event) {
        return event.getPlantationId() != null && !event.getPlantationId().isBlank();
    }

    private OffsetDateTime resolveOccurredAt(PlantationAssignmentEvent event) {
        return event.getOccurredAt() != null ? event.getOccurredAt() : OffsetDateTime.now();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private UUID parseUuid(String value) {
        return UUID.fromString(value);
    }
}

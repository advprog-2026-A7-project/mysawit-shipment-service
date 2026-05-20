package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class PlantationAssignmentEvent {
    private String eventId;

    @JsonAlias({"userId", "mandorId", "supirId", "workerId"})
    private String userId;

    @JsonAlias({"name", "username", "supirName", "mandorName"})
    private String name;

    private String role;

    @JsonAlias({"plantationId", "kebunId"})
    private String plantationId;

    private AssignmentAction action;
    private OffsetDateTime occurredAt;

    public PlantationAssignmentEvent(
            String eventId,
            Object userId,
            String name,
            String role,
            String plantationId,
            Object action,
            OffsetDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.userId = stringify(userId);
        this.name = name;
        this.role = role;
        this.plantationId = plantationId;
        this.action = parseAction(action);
        this.occurredAt = occurredAt;
    }

    public String eventId() {
        return eventId;
    }

    public UUID userId() {
        return userId == null ? null : UUID.fromString(userId);
    }

    public String name() {
        return name;
    }

    public String role() {
        return role;
    }

    public String plantationId() {
        return plantationId;
    }

    public String action() {
        return action == null ? null : action.name();
    }

    public OffsetDateTime occurredAt() {
        return occurredAt;
    }

    public enum AssignmentAction {
        ASSIGNED,
        UNASSIGNED,
        REASSIGNED
    }

    private static String stringify(Object userId) {
        return userId == null ? null : userId.toString();
    }

    private static AssignmentAction parseAction(Object action) {
        if (action == null) {
            return null;
        }
        if (action instanceof AssignmentAction assignmentAction) {
            return assignmentAction;
        }
        String actionValue = action.toString();
        if (actionValue.isBlank()) {
            return null;
        }
        return AssignmentAction.valueOf(actionValue.trim().toUpperCase(Locale.ROOT));
    }
}

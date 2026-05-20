package com.mysawit.shipment.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAssignmentEvent {
    private String userId;
    private String mandorId;
    private String mandorName;
    private AssignmentAction action;
    private Instant occurredAt;

    public enum AssignmentAction {
        ASSIGNED,
        UNASSIGNED,
        REASSIGNED
    }
}

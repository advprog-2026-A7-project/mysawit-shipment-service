package com.mysawit.shipment.event;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.mysawit.shipment.service.UserReplicaService;

class UserAssignmentEventConsumerTest {

    @Test
    void onUserAssignmentDelegatesToReplicaService() {
        UserReplicaService userReplicaService = mock(UserReplicaService.class);
        UserAssignmentEventConsumer consumer = new UserAssignmentEventConsumer(userReplicaService);
        UserAssignmentEvent event = new UserAssignmentEvent(
                "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222",
                "Mandor Demo",
                UserAssignmentEvent.AssignmentAction.ASSIGNED,
                Instant.now()
        );

        consumer.onUserAssignment(event);

        verify(userReplicaService).applyAssignment(event);
    }
}

package com.mysawit.shipment.event;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.mysawit.shipment.service.UserReplicaService;

class UserDeletedEventConsumerTest {

    @Test
    void onUserDeletedDelegatesToReplicaService() {
        UserReplicaService userReplicaService = mock(UserReplicaService.class);
        UserDeletedEventConsumer consumer = new UserDeletedEventConsumer(userReplicaService);
        UserDeletedEvent event = new UserDeletedEvent(
                "11111111-1111-1111-1111-111111111111",
                "SUPIR",
                null,
                Instant.now()
        );

        consumer.onUserDeleted(event);

        verify(userReplicaService).markDeleted(event);
    }
}

package com.mysawit.shipment.event;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.mysawit.shipment.service.UserReplicaService;

class UserUpdatedEventConsumerTest {

    @Test
    void onUserUpdatedDelegatesToReplicaService() {
        UserReplicaService userReplicaService = mock(UserReplicaService.class);
        UserUpdatedEventConsumer consumer = new UserUpdatedEventConsumer(userReplicaService);
        UserUpdatedEvent event = new UserUpdatedEvent(
                "11111111-1111-1111-1111-111111111111",
                "user@mysawit.local",
                "SUPIR",
                "supir-demo",
                "Supir Demo",
                Instant.now()
        );

        consumer.onUserUpdated(event);

        verify(userReplicaService).upsertFromUpdate(event);
    }
}

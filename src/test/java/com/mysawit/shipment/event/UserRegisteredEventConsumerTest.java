package com.mysawit.shipment.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.mysawit.shipment.service.UserReplicaService;

class UserRegisteredEventConsumerTest {

    @Test
    void onUserRegisteredDelegatesToReplicaService() {
        UserReplicaService userReplicaService = mock(UserReplicaService.class);
        UserRegisteredEventConsumer consumer = new UserRegisteredEventConsumer(userReplicaService);
        UserRegisteredEvent event = new UserRegisteredEvent(
                "11111111-1111-1111-1111-111111111111",
                "user@mysawit.local",
                "SUPIR",
                "supir-demo"
        );

        consumer.onUserRegistered(event);

        verify(userReplicaService).upsertFromRegistration(event);
    }
}

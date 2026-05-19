package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.mysawit.shipment.service.HarvestReplicaService;

class HarvestEventConsumerTest {

    @Test
    void onHarvestEventDelegatesToReplicaService() {
        HarvestReplicaService harvestReplicaService = mock(HarvestReplicaService.class);
        HarvestEventConsumer consumer = new HarvestEventConsumer(harvestReplicaService);
        HarvestEvent event = new HarvestEvent(
                "event-1",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "00000000-0000-0000-0000-000000000001",
                100.0,
                "PENDING",
                OffsetDateTime.now()
        );

        consumer.onHarvestEvent(event);

        verify(harvestReplicaService).upsertHarvest(event);
    }
}

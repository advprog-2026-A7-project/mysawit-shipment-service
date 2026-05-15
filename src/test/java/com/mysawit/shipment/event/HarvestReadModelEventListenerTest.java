package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.shipment.model.HarvestReadModel;
import com.mysawit.shipment.repository.HarvestReadModelRepository;

class HarvestReadModelEventListenerTest {

    private static final UUID HARVEST_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MANDOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-04-14T10:00:00Z");
    private static final String PLANTATION_ID = "plantation-1";
    private static final String STATUS_APPROVED = "APPROVED";

    private HarvestReadModelRepository harvestReadModelRepository;
    private HarvestReadModelEventListener listener;

    @BeforeEach
    void setUp() {
        harvestReadModelRepository = mock(HarvestReadModelRepository.class);
        listener = new HarvestReadModelEventListener(harvestReadModelRepository);
    }

    @Test
    void handleHarvestEventSavesNormalizedReadModel() {
        HarvestShipmentEvent event = new HarvestShipmentEvent(
                "event-1",
                HARVEST_ID,
                MANDOR_ID,
                PLANTATION_ID,
                120.0,
                "approved",
                OCCURRED_AT
        );
        when(harvestReadModelRepository.findById(HARVEST_ID)).thenReturn(Optional.empty());

        listener.handleHarvestEvent(event);

        ArgumentCaptor<HarvestReadModel> captor = ArgumentCaptor.forClass(HarvestReadModel.class);
        verify(harvestReadModelRepository).save(captor.capture());
        HarvestReadModel harvest = captor.getValue();
        assertEquals(HARVEST_ID, harvest.getHarvestId());
        assertEquals(MANDOR_ID, harvest.getMandorUserId());
        assertEquals(PLANTATION_ID, harvest.getPlantationId());
        assertEquals(STATUS_APPROVED, harvest.getStatus());
        assertEquals(120.0, harvest.getWeightKg());
        assertEquals("event-1", harvest.getLastEventId());
        assertEquals(OCCURRED_AT, harvest.getUpdatedAt());
    }

    @Test
    void handleHarvestEventUpdatesExistingAndDefaultsTimestamp() {
        HarvestReadModel existing = new HarvestReadModel();
        when(harvestReadModelRepository.findById(HARVEST_ID)).thenReturn(Optional.of(existing));

        listener.handleHarvestEvent(new HarvestShipmentEvent(
                "event-2",
                HARVEST_ID,
                MANDOR_ID,
                PLANTATION_ID,
                130.0,
                STATUS_APPROVED,
                null
        ));

        verify(harvestReadModelRepository).save(existing);
        assertEquals(HARVEST_ID, existing.getHarvestId());
        assertEquals(130.0, existing.getWeightKg());
    }

    @Test
    void handleHarvestEventIgnoresInvalidPayloads() {
        listener.handleHarvestEvent(null);
        listener.handleHarvestEvent(new HarvestShipmentEvent("e", null, MANDOR_ID, PLANTATION_ID, 120.0, STATUS_APPROVED, null));
        listener.handleHarvestEvent(new HarvestShipmentEvent("e", HARVEST_ID, null, PLANTATION_ID, 120.0, STATUS_APPROVED, null));
        listener.handleHarvestEvent(new HarvestShipmentEvent("e", HARVEST_ID, MANDOR_ID, " ", 120.0, STATUS_APPROVED, null));
        listener.handleHarvestEvent(new HarvestShipmentEvent("e", HARVEST_ID, MANDOR_ID, PLANTATION_ID, null, STATUS_APPROVED, null));
        listener.handleHarvestEvent(new HarvestShipmentEvent("e", HARVEST_ID, MANDOR_ID, PLANTATION_ID, 120.0, " ", null));

        verify(harvestReadModelRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

package com.mysawit.shipment.client;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mysawit.shipment.model.HarvestReadModel;
import com.mysawit.shipment.repository.HarvestReadModelRepository;

class LocalHarvestServiceClientTest {

    private static final UUID HARVEST_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MANDOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private HarvestReadModelRepository harvestReadModelRepository;
    private LocalHarvestServiceClient localHarvestServiceClient;

    @BeforeEach
    void setUp() {
        harvestReadModelRepository = mock(HarvestReadModelRepository.class);
        localHarvestServiceClient = new LocalHarvestServiceClient(harvestReadModelRepository);
    }

    @Test
    void getHarvestByIdMapsLocalReadModel() {
        HarvestReadModel harvest = new HarvestReadModel();
        harvest.setHarvestId(HARVEST_ID);
        harvest.setMandorUserId(MANDOR_ID);
        harvest.setPlantationId("plantation-1");
        harvest.setStatus("APPROVED");
        harvest.setWeightKg(120.0);
        when(harvestReadModelRepository.findById(HARVEST_ID)).thenReturn(Optional.of(harvest));

        HarvestServiceClient.HarvestDetails details = localHarvestServiceClient.getHarvestById(MANDOR_ID, HARVEST_ID);

        assertEquals(HARVEST_ID, details.id());
        assertEquals(MANDOR_ID, details.mandorUserId());
        assertEquals("plantation-1", details.plantationId());
        assertEquals("APPROVED", details.status());
        assertEquals(120.0, details.weightKg());
    }

    @Test
    void getHarvestByIdReturnsNullWhenMissing() {
        when(harvestReadModelRepository.findById(HARVEST_ID)).thenReturn(Optional.empty());

        assertNull(localHarvestServiceClient.getHarvestById(MANDOR_ID, HARVEST_ID));
    }
}

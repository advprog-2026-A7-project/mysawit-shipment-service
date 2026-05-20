package com.mysawit.shipment.service;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.mysawit.shipment.event.HarvestEvent;

class HarvestReplicaServiceTest {

    private static final UUID HARVEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HARVESTER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FOREMAN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-05-19T01:00:00Z");
    private static final String APPROVED_STATUS = "APPROVED";

    private JdbcTemplate jdbcTemplate;
    private HarvestReplicaService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new HarvestReplicaService(jdbcTemplate);
    }

    @Test
    void upsertHarvestStoresHarvestPayload() {
        HarvestEvent event = new HarvestEvent(
                "event-1",
                HARVEST_ID,
                HARVESTER_ID,
                FOREMAN_ID,
                "00000000-0000-0000-0000-000000000001",
                100.0,
                "APPROVED",
                OCCURRED_AT
        );

        service.upsertHarvest(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(HARVEST_ID),
                eq("event-1"),
                eq(HARVESTER_ID),
                eq(FOREMAN_ID),
                eq("00000000-0000-0000-0000-000000000001"),
                eq(100.0),
                eq("APPROVED"),
                eq(OCCURRED_AT)
        );
    }

    @Test
    void getHarvestByIdReturnsReplicaHarvestAndNormalizesStatus() {
        mockReplicaQuery(HARVEST_ID, APPROVED_STATUS, true);

        HarvestReplicaService.HarvestDetails result = service.getHarvestById(FOREMAN_ID, HARVEST_ID);

        assertEquals(HARVEST_ID, result.id());
        assertEquals("Approved", result.status());
    }

    @Test
    void getHarvestByIdReturnsNullWhenReplicaIsMissing() {
        mockReplicaQuery(HARVEST_ID, APPROVED_STATUS, false);

        HarvestReplicaService.HarvestDetails result = service.getHarvestById(FOREMAN_ID, HARVEST_ID);

        assertNull(result);
    }

    @Test
    void getHarvestByIdReturnsNullStatusWhenReplicaStatusIsNull() {
        mockReplicaQuery(HARVEST_ID, null, true);

        HarvestReplicaService.HarvestDetails result = service.getHarvestById(FOREMAN_ID, HARVEST_ID);

        assertNull(result.status());
    }

    @Test
    void getHarvestByIdKeepsUnknownStatus() {
        mockReplicaQuery(HARVEST_ID, "CUSTOM", true);

        HarvestReplicaService.HarvestDetails result = service.getHarvestById(FOREMAN_ID, HARVEST_ID);

        assertEquals("CUSTOM", result.status());
    }

    @SuppressWarnings("unchecked")
    private void mockReplicaQuery(UUID harvestId, String status, boolean found) {
        when(jdbcTemplate.query(
                anyString(),
                org.mockito.ArgumentMatchers.any(ResultSetExtractor.class),
                eq(harvestId),
                eq(FOREMAN_ID)
        )).thenAnswer(invocation -> {
            ResultSetExtractor<HarvestReplicaService.HarvestDetails> extractor = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(found);
            if (found) {
                when(resultSet.getObject("id", UUID.class)).thenReturn(harvestId);
                when(resultSet.getString("status")).thenReturn(status);
            }
            return extractor.extractData(resultSet);
        });
    }
}

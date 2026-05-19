package com.mysawit.shipment.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mysawit.shipment.client.HarvestServiceClient;
import com.mysawit.shipment.event.HarvestEvent;

@Service
@Primary
public class HarvestReplicaService implements HarvestServiceClient {

    private static final String UPSERT_HARVEST = """
            insert into public.shipment_harvest_replicas (
                id, event_id, harvester_id, foreman_id, plantation_id,
                weight_kg, status, approved_at, created_at, updated_at
            )
            values (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            on conflict (id) do update set
                event_id = excluded.event_id,
                harvester_id = excluded.harvester_id,
                foreman_id = excluded.foreman_id,
                plantation_id = excluded.plantation_id,
                weight_kg = excluded.weight_kg,
                status = excluded.status,
                approved_at = excluded.approved_at,
                updated_at = now()
            """;
    private static final String FIND_HARVEST_BY_ID_AND_FOREMAN = """
            select id, foreman_id, plantation_id, status, weight_kg
            from public.shipment_harvest_replicas
            where id = ? and foreman_id = ?
            """;
    private static final Map<String, String> NORMALIZED_STATUSES = Map.of(
            "PENDING", "Pending",
            "APPROVED", "Approved",
            "REJECTED", "Rejected"
    );

    private final JdbcTemplate jdbcTemplate;

    public HarvestReplicaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertHarvest(HarvestEvent event) {
        jdbcTemplate.update(
                UPSERT_HARVEST,
                event.getHarvestId(),
                event.getEventId(),
                event.getHarvesterId(),
                event.getForemanId(),
                event.getPlantationId(),
                event.getWeight(),
                event.getStatus(),
                event.getOccurredAt()
        );
    }

    @Override
    public HarvestDetails getHarvestById(UUID foremanId, UUID harvestId) {
        return jdbcTemplate.query(
                FIND_HARVEST_BY_ID_AND_FOREMAN,
                resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return new HarvestDetails(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject("foreman_id", UUID.class),
                            resultSet.getString("plantation_id"),
                            normalizeStatus(resultSet.getString("status")),
                            resultSet.getDouble("weight_kg")
                    );
                },
                harvestId,
                foremanId
        );
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null) {
            return null;
        }
        return NORMALIZED_STATUSES.getOrDefault(rawStatus, rawStatus);
    }
}

package com.mysawit.shipment.service;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mysawit.shipment.event.PlantationAssignmentEvent;

@Service
public class PlantationAssignmentReplicaService {

    private static final String UPSERT_PLANTATION_ASSIGNMENT = """
            insert into public.shipment_user_replicas (
                id, username, role, plantation_id, deleted, created_at, updated_at
            )
            values (?, ?, ?, ?, false, now(), now())
            on conflict (id) do update set
                username = coalesce(excluded.username, shipment_user_replicas.username),
                role = coalesce(excluded.role, shipment_user_replicas.role),
                plantation_id = excluded.plantation_id,
                deleted = false,
                updated_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public PlantationAssignmentReplicaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void applyAssignment(PlantationAssignmentEvent event) {
        jdbcTemplate.update(
                UPSERT_PLANTATION_ASSIGNMENT,
                parseUuid(event.getUserId()),
                event.getName(),
                event.getRole(),
                assignedPlantationId(event)
        );
    }

    private String assignedPlantationId(PlantationAssignmentEvent event) {
        if (PlantationAssignmentEvent.AssignmentAction.UNASSIGNED.equals(event.getAction())) {
            return null;
        }
        return event.getPlantationId();
    }

    private UUID parseUuid(String value) {
        return UUID.fromString(value);
    }
}

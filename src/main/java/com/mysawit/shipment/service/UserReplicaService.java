package com.mysawit.shipment.service;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mysawit.shipment.event.UserAssignmentEvent;
import com.mysawit.shipment.event.UserDeletedEvent;
import com.mysawit.shipment.event.UserRegisteredEvent;
import com.mysawit.shipment.event.UserUpdatedEvent;

@Service
public class UserReplicaService {

    private static final String UPSERT_REGISTERED_USER = """
            insert into public.shipment_user_replicas (
                id, email, username, role, deleted, created_at, updated_at
            )
            values (?, ?, ?, ?, false, now(), now())
            on conflict (id) do update set
                email = excluded.email,
                username = excluded.username,
                role = excluded.role,
                deleted = false,
                updated_at = now()
            """;

    private static final String UPSERT_USER_ASSIGNMENT = """
            insert into public.shipment_user_replicas (
                id, role, mandor_id, mandor_name, deleted, created_at, updated_at
            )
            values (?, 'BURUH', ?, ?, false, now(), now())
            on conflict (id) do update set
                role = coalesce(shipment_user_replicas.role, excluded.role),
                mandor_id = excluded.mandor_id,
                mandor_name = excluded.mandor_name,
                deleted = false,
                updated_at = now()
            """;

    private static final String UPSERT_UPDATED_USER = """
            insert into public.shipment_user_replicas (
                id, email, username, role, deleted, created_at, updated_at
            )
            values (?, ?, ?, ?, false, now(), now())
            on conflict (id) do update set
                email = coalesce(excluded.email, shipment_user_replicas.email),
                username = coalesce(excluded.username, shipment_user_replicas.username),
                role = coalesce(excluded.role, shipment_user_replicas.role),
                mandor_id = case
                    when upper(coalesce(excluded.role, shipment_user_replicas.role, '')) = 'BURUH'
                    then shipment_user_replicas.mandor_id
                    else null
                end,
                mandor_name = case
                    when upper(coalesce(excluded.role, shipment_user_replicas.role, '')) = 'BURUH'
                    then shipment_user_replicas.mandor_name
                    else null
                end,
                plantation_id = case
                    when upper(coalesce(excluded.role, shipment_user_replicas.role, '')) in ('MANDOR', 'SUPIR')
                    then shipment_user_replicas.plantation_id
                    else null
                end,
                deleted = false,
                updated_at = now()
            """;

    private static final String MARK_USER_DELETED = """
            update public.shipment_user_replicas
            set deleted = true,
                mandor_id = null,
                mandor_name = null,
                plantation_id = null,
                updated_at = now()
            where id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserReplicaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertFromRegistration(UserRegisteredEvent event) {
        jdbcTemplate.update(
                UPSERT_REGISTERED_USER,
                parseUuid(event.getUserId()),
                event.getEmail(),
                event.getUsername(),
                event.getRole()
        );
    }

    public void applyAssignment(UserAssignmentEvent event) {
        UUID mandorId = shouldClearMandor(event)
                ? null
                : parseNullableUuid(event.getMandorId());
        String mandorName = shouldClearMandor(event) ? null : event.getMandorName();

        jdbcTemplate.update(
                UPSERT_USER_ASSIGNMENT,
                parseUuid(event.getUserId()),
                mandorId,
                mandorName
        );
    }

    public void upsertFromUpdate(UserUpdatedEvent event) {
        jdbcTemplate.update(
                UPSERT_UPDATED_USER,
                parseUuid(event.getUserId()),
                event.getEmail(),
                displayName(event),
                event.getRole()
        );
    }

    public void markDeleted(UserDeletedEvent event) {
        jdbcTemplate.update(MARK_USER_DELETED, parseUuid(event.getUserId()));
    }

    private boolean shouldClearMandor(UserAssignmentEvent event) {
        return UserAssignmentEvent.AssignmentAction.UNASSIGNED.equals(event.getAction());
    }

    private UUID parseNullableUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseUuid(value);
    }

    private String displayName(UserUpdatedEvent event) {
        if (event.getUsername() != null && !event.getUsername().isBlank()) {
            return event.getUsername();
        }
        return event.getName();
    }

    private UUID parseUuid(String value) {
        return UUID.fromString(value);
    }
}

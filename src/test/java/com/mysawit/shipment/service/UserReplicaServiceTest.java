package com.mysawit.shipment.service;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mysawit.shipment.event.UserAssignmentEvent;
import com.mysawit.shipment.event.UserDeletedEvent;
import com.mysawit.shipment.event.UserRegisteredEvent;

class UserReplicaServiceTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MANDOR_ID = "22222222-2222-2222-2222-222222222222";

    private JdbcTemplate jdbcTemplate;
    private UserReplicaService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new UserReplicaService(jdbcTemplate);
    }

    @Test
    void upsertFromRegistrationStoresUserProfile() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                USER_ID,
                "supir@mysawit.local",
                "SUPIR",
                "supir-demo"
        );

        service.upsertFromRegistration(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(UUID.fromString(USER_ID)),
                eq("supir@mysawit.local"),
                eq("supir-demo"),
                eq("SUPIR")
        );
    }

    @Test
    void applyAssignmentStoresMandorData() {
        UserAssignmentEvent event = new UserAssignmentEvent(
                USER_ID,
                MANDOR_ID,
                "Mandor Demo",
                UserAssignmentEvent.AssignmentAction.ASSIGNED,
                Instant.now()
        );

        service.applyAssignment(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(UUID.fromString(USER_ID)),
                eq(UUID.fromString(MANDOR_ID)),
                eq("Mandor Demo")
        );
    }

    @Test
    void applyAssignmentAllowsBlankMandorId() {
        UserAssignmentEvent event = new UserAssignmentEvent(
                USER_ID,
                " ",
                null,
                UserAssignmentEvent.AssignmentAction.REASSIGNED,
                Instant.now()
        );

        service.applyAssignment(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(UUID.fromString(USER_ID)),
                isNull(),
                isNull()
        );
    }

    @Test
    void applyAssignmentAllowsNullMandorId() {
        UserAssignmentEvent event = new UserAssignmentEvent(
                USER_ID,
                null,
                null,
                UserAssignmentEvent.AssignmentAction.REASSIGNED,
                Instant.now()
        );

        service.applyAssignment(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(UUID.fromString(USER_ID)),
                isNull(),
                isNull()
        );
    }

    @Test
    void applyAssignmentClearsMandorWhenUnassigned() {
        UserAssignmentEvent event = new UserAssignmentEvent(
                USER_ID,
                MANDOR_ID,
                "Mandor Demo",
                UserAssignmentEvent.AssignmentAction.UNASSIGNED,
                Instant.now()
        );

        service.applyAssignment(event);

        verify(jdbcTemplate).update(
                anyString(),
                eq(UUID.fromString(USER_ID)),
                isNull(),
                isNull()
        );
    }

    @Test
    void markDeletedMarksReplicaAsDeleted() {
        UserDeletedEvent event = new UserDeletedEvent(USER_ID, "SUPIR", null, Instant.now());

        service.markDeleted(event);

        verify(jdbcTemplate).update(anyString(), eq(UUID.fromString(USER_ID)));
    }
}

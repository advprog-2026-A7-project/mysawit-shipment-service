package com.mysawit.shipment.service;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;

class WorkerAssignmentLookupServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String ROLE_SUPIR = "SUPIR";

    private WorkerPlantationAssignmentRepository repository;
    private WorkerAssignmentLookupService service;

    @BeforeEach
    void setUp() {
        repository = mock(WorkerPlantationAssignmentRepository.class);
        service = new WorkerAssignmentLookupService(repository);
    }

    @Test
    void findByUserIdAndRoleDelegatesToRepository() {
        WorkerPlantationAssignment assignment = new WorkerPlantationAssignment();
        Optional<WorkerPlantationAssignment> expected = Optional.of(assignment);
        when(repository.findByUserIdAndRole(USER_ID, ROLE_SUPIR)).thenReturn(expected);

        Optional<WorkerPlantationAssignment> result = service.findByUserIdAndRole(USER_ID, ROLE_SUPIR);

        assertSame(expected, result);
        verify(repository).findByUserIdAndRole(USER_ID, ROLE_SUPIR);
    }

    @Test
    void cacheEvictionMethodsAreCallable() {
        service.evictUser(USER_ID);
        service.evictAll();
    }
}

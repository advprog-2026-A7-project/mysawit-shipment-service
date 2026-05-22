package com.mysawit.shipment.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;

/**
 * Cached lookup for worker plantation assignments.
 * Hot path: every shipment write touches this twice (mandor + supir),
 * and assignments rarely change. Cache invalidated by event listener
 * via {@link #evictUser(UUID)} whenever an assignment is upserted/removed.
 */
@Service
public class WorkerAssignmentLookupService {

    public static final String CACHE_NAME = "workerAssignments";

    private final WorkerPlantationAssignmentRepository repository;

    public WorkerAssignmentLookupService(WorkerPlantationAssignmentRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = CACHE_NAME, key = "#userId + ':' + #role")
    @Transactional(readOnly = true)
    public Optional<WorkerPlantationAssignment> findByUserIdAndRole(UUID userId, String role) {
        return repository.findByUserIdAndRole(userId, role);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void evictUser(UUID userId) {
        // allEntries = true keeps invalidation simple regardless of role transitions.
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void evictAll() {
        // exposed for tests / admin operations.
    }
}

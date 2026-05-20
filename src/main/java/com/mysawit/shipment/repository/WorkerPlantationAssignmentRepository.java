package com.mysawit.shipment.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mysawit.shipment.model.WorkerPlantationAssignment;

@Repository
public interface WorkerPlantationAssignmentRepository extends JpaRepository<WorkerPlantationAssignment, UUID> {

    Optional<WorkerPlantationAssignment> findByUserIdAndRole(UUID userId, String role);

    @Query(value = "SELECT * FROM worker_plantation_assignments a WHERE "
            + "a.role = CAST(:role AS TEXT) "
            + "AND a.plantation_id = CAST(:plantationId AS TEXT) "
            + "AND (CAST(:name AS TEXT) IS NULL OR a.name ILIKE CONCAT('%', CAST(:name AS TEXT), '%')) "
            + "ORDER BY a.name ASC",
            nativeQuery = true)
    List<WorkerPlantationAssignment> findByRoleAndPlantationIdAndName(
            @Param("role") String role,
            @Param("plantationId") String plantationId,
            @Param("name") String name
    );
}

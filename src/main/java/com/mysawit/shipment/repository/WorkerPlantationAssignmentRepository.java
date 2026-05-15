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

    @Query("SELECT a FROM WorkerPlantationAssignment a WHERE a.role = :role "
            + "AND a.plantationId = :plantationId "
            + "AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) "
            + "ORDER BY a.name ASC")
    List<WorkerPlantationAssignment> findByRoleAndPlantationIdAndName(
            @Param("role") String role,
            @Param("plantationId") String plantationId,
            @Param("name") String name
    );
}

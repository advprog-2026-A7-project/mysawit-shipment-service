package com.mysawit.shipment.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findBySupirUserId(UUID supirUserId);

    boolean existsByItemsHarvestId(UUID harvestId);

    @Query("SELECT s FROM Shipment s WHERE "
            + "(:supirUserId IS NULL OR s.supirUserId = :supirUserId) "
            + "AND (:mandorUserId IS NULL OR s.mandorUserId = :mandorUserId) "
            + "AND (:status IS NULL OR s.status = :status) "
            + "AND (:mandorName IS NULL OR LOWER(s.mandorName) LIKE LOWER(CONCAT('%', :mandorName, '%'))) "
            + "AND (:supirName IS NULL OR LOWER(s.supirName) LIKE LOWER(CONCAT('%', :supirName, '%'))) "
            + "AND (:from IS NULL OR s.createdAt >= :from) "
            + "AND (:to IS NULL OR s.createdAt < :to) "
            + "ORDER BY s.createdAt DESC")
    List<Shipment> findWithFilters(
            @Param("supirUserId") UUID supirUserId,
            @Param("mandorUserId") UUID mandorUserId,
            @Param("status") ShipmentStatus status,
            @Param("mandorName") String mandorName,
            @Param("supirName") String supirName,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}

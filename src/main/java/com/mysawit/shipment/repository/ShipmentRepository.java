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

    @Query(value = "SELECT * FROM shipments s WHERE "
            + "(CAST(:supirUserId AS TEXT) IS NULL OR s.supir_user_id = CAST(:supirUserId AS UUID)) "
            + "AND (CAST(:mandorUserId AS TEXT) IS NULL OR s.mandor_user_id = CAST(:mandorUserId AS UUID)) "
            + "AND (CAST(:status AS TEXT) IS NULL OR s.status = CAST(:status AS TEXT)) "
            + "AND (CAST(:mandorName AS TEXT) IS NULL OR s.mandor_name ILIKE CONCAT('%', CAST(:mandorName AS TEXT), '%')) "
            + "AND (CAST(:supirName AS TEXT) IS NULL OR s.supir_name ILIKE CONCAT('%', CAST(:supirName AS TEXT), '%')) "
            + "AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR s.created_at >= CAST(:from AS TIMESTAMPTZ)) "
            + "AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR s.created_at < CAST(:to AS TIMESTAMPTZ)) "
            + "ORDER BY s.created_at DESC",
            nativeQuery = true)
    List<Shipment> findWithFilters(
            @Param("supirUserId") String supirUserId,
            @Param("mandorUserId") String mandorUserId,
            @Param("status") String status,
            @Param("mandorName") String mandorName,
            @Param("supirName") String supirName,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}

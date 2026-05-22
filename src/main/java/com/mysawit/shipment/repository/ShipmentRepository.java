package com.mysawit.shipment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mysawit.shipment.model.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID>, JpaSpecificationExecutor<Shipment> {

    @EntityGraph(attributePaths = "items")
    List<Shipment> findBySupirUserId(UUID supirUserId);

    @EntityGraph(attributePaths = "items")
    Optional<Shipment> findWithItemsById(UUID id);

    boolean existsByItemsHarvestId(UUID harvestId);

    @Query("SELECT DISTINCT si.harvestId FROM ShipmentItem si WHERE si.harvestId IN :harvestIds")
    List<UUID> findClaimedHarvestIds(@Param("harvestIds") Collection<UUID> harvestIds);

    /**
     * @deprecated retained for legacy tests only. New code uses {@code findAll(Specification)}
     * via {@link com.mysawit.shipment.repository.ShipmentSpecifications}.
     */
    @Deprecated
    default List<Shipment> findWithFilters(
            String supirUserId,
            String mandorUserId,
            String status,
            String mandorName,
            String supirName,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to
    ) {
        UUID supir = supirUserId == null ? null : UUID.fromString(supirUserId);
        UUID mandor = mandorUserId == null ? null : UUID.fromString(mandorUserId);
        com.mysawit.shipment.domain.ShipmentStatus statusEnum =
                status == null ? null : com.mysawit.shipment.domain.ShipmentStatus.valueOf(status);
        return findAll(org.springframework.data.jpa.domain.Specification
                .where(com.mysawit.shipment.repository.ShipmentSpecifications.bySupirUserId(supir))
                .and(com.mysawit.shipment.repository.ShipmentSpecifications.byMandorUserId(mandor))
                .and(com.mysawit.shipment.repository.ShipmentSpecifications.byStatus(statusEnum))
                .and(com.mysawit.shipment.repository.ShipmentSpecifications.byMandorNameLike(mandorName))
                .and(com.mysawit.shipment.repository.ShipmentSpecifications.bySupirNameLike(supirName))
                .and(com.mysawit.shipment.repository.ShipmentSpecifications.createdBetween(from, to)));
    }
}

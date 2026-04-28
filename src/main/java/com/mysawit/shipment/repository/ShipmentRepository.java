package com.mysawit.shipment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mysawit.shipment.model.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findBySupirUserId(UUID supirUserId);

    boolean existsByItemsHarvestId(UUID harvestId);
}

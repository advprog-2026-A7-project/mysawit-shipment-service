package com.mysawit.shipment.repository;

import com.mysawit.shipment.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    java.util.List<Shipment> findBySupirUserId(UUID supirUserId);
}

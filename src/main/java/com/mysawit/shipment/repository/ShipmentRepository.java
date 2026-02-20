package com.mysawit.shipment.repository;

import com.mysawit.shipment.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByHarvestId(Long harvestId);
    List<Shipment> findByStatus(String status);
    List<Shipment> findByDestination(String destination);
}

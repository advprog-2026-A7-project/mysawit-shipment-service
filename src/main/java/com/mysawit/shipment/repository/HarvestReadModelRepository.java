package com.mysawit.shipment.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mysawit.shipment.model.HarvestReadModel;

@Repository
public interface HarvestReadModelRepository extends JpaRepository<HarvestReadModel, UUID> {
}

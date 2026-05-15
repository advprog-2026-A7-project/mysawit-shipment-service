package com.mysawit.shipment.client;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mysawit.shipment.repository.HarvestReadModelRepository;

@Service
public class LocalHarvestServiceClient implements HarvestServiceClient {

    private final HarvestReadModelRepository harvestReadModelRepository;

    public LocalHarvestServiceClient(HarvestReadModelRepository harvestReadModelRepository) {
        this.harvestReadModelRepository = harvestReadModelRepository;
    }

    @Override
    public HarvestDetails getHarvestById(UUID foremanId, UUID harvestId) {
        return harvestReadModelRepository.findById(harvestId)
                .map(harvest -> new HarvestDetails(
                        harvest.getHarvestId(),
                        harvest.getMandorUserId(),
                        harvest.getPlantationId(),
                        harvest.getStatus(),
                        harvest.getWeightKg()
                ))
                .orElse(null);
    }
}

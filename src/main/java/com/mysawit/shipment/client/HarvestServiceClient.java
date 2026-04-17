package com.mysawit.shipment.client;

import java.util.UUID;

public interface HarvestServiceClient {

    HarvestDetails getHarvestById(UUID foremanId, UUID harvestId);

    record HarvestDetails(UUID id, String status) {
    }
}

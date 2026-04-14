package com.mysawit.shipment.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HarvestServiceClient {

    Map<UUID, HarvestDetails> getHarvestsByIds(UUID foremanId, List<UUID> harvestIds);

    record HarvestDetails(UUID id, String status) {
    }
}

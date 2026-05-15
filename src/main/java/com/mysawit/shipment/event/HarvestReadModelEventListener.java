package com.mysawit.shipment.event;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mysawit.shipment.config.RabbitMqConfig;
import com.mysawit.shipment.model.HarvestReadModel;
import com.mysawit.shipment.repository.HarvestReadModelRepository;

@Component
public class HarvestReadModelEventListener {

    private final HarvestReadModelRepository harvestReadModelRepository;

    public HarvestReadModelEventListener(HarvestReadModelRepository harvestReadModelRepository) {
        this.harvestReadModelRepository = harvestReadModelRepository;
    }

    @Transactional
    @RabbitListener(queues = RabbitMqConfig.HARVEST_EVENTS_QUEUE)
    public void handleHarvestEvent(HarvestShipmentEvent event) {
        if (!isUsable(event)) {
            return;
        }

        HarvestReadModel harvest = harvestReadModelRepository.findById(event.harvestId())
                .orElseGet(HarvestReadModel::new);
        harvest.setHarvestId(event.harvestId());
        harvest.setMandorUserId(event.mandorUserId());
        harvest.setPlantationId(event.plantationId());
        harvest.setWeightKg(event.weightKg());
        harvest.setStatus(normalizeStatus(event.status()));
        harvest.setLastEventId(event.eventId());
        harvest.setUpdatedAt(resolveOccurredAt(event));

        harvestReadModelRepository.save(harvest);
    }

    private boolean isUsable(HarvestShipmentEvent event) {
        return event != null
                && event.harvestId() != null
                && event.mandorUserId() != null
                && event.plantationId() != null
                && !event.plantationId().isBlank()
                && event.weightKg() != null
                && event.status() != null
                && !event.status().isBlank();
    }

    private String normalizeStatus(String status) {
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime resolveOccurredAt(HarvestShipmentEvent event) {
        return event.occurredAt() != null ? event.occurredAt() : OffsetDateTime.now();
    }
}

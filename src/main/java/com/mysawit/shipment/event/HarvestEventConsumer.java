package com.mysawit.shipment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mysawit.shipment.service.HarvestReplicaService;

@Component
public class HarvestEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(HarvestEventConsumer.class);

    private final HarvestReplicaService harvestReplicaService;

    public HarvestEventConsumer(HarvestReplicaService harvestReplicaService) {
        this.harvestReplicaService = harvestReplicaService;
    }

    @RabbitListener(queues = "${shipment.rabbitmq.queues.harvest-events:shipment.harvest-events.queue}")
    public void onHarvestEvent(HarvestEvent event) {
        log.debug("Received harvest event harvestId={} status={}", event.getHarvestId(), event.getStatus());
        harvestReplicaService.upsertHarvest(event);
    }
}

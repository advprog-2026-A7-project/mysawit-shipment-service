package com.mysawit.shipment.event;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.mysawit.shipment.model.Shipment;

@Service
public class ShipmentEventPublisher {

    private static final String EXCHANGE = "shipment.exchange";
    private static final String ROUTING_KEY = "shipment.completed";

    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    @Autowired
    public ShipmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this(rabbitTemplate, Clock.systemDefaultZone());
    }

    ShipmentEventPublisher(RabbitTemplate rabbitTemplate, Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    public void publishShipmentCompleted(Shipment shipment) {
        ShipmentCompletedEvent event = new ShipmentCompletedEvent(
                shipment.getId(),
                shipment.getSupirUserId(),
                shipment.getMandorUserId(),
                shipment.getTotalKg(),
                shipment.getItems().stream()
                        .map(item -> item.getHarvestId())
                        .toList(),
                LocalDateTime.now(clock)
        );

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}

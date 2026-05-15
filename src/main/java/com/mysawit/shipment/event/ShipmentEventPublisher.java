package com.mysawit.shipment.event;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.ShipmentItem;

@Service
public class ShipmentEventPublisher {

    private static final String EXCHANGE = "shipment.exchange";
    private static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    private static final String ROUTING_KEY_COMPLETED = "shipment.completed";
    private static final String ROUTING_KEY_APPROVED_BY_MANDOR = "shipment.approved-by-mandor";
    private static final String ROUTING_KEY_APPROVED_BY_ADMIN = "shipment.approved-by-admin";
    private static final String ROUTING_KEY_NOTIFICATION_CREATED = "notification.created";
    private static final String ROLE_SUPIR = "SUPIR";
    private static final String ROLE_MANDOR = "MANDOR";

    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    @Value("${shipment.events.enabled:true}")
    private boolean eventsEnabled = true;

    @Autowired
    public ShipmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this(rabbitTemplate, Clock.systemDefaultZone());
    }

    ShipmentEventPublisher(RabbitTemplate rabbitTemplate, Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    public void publishShipmentCompleted(Shipment shipment) {
        if (!eventsEnabled) {
            return;
        }
        ShipmentCompletedEvent event = new ShipmentCompletedEvent(
                shipment.getId(),
                shipment.getSupirUserId(),
                shipment.getMandorUserId(),
                shipment.getTotalKg(),
                shipment.getItems().stream()
                        .map(ShipmentItem::getHarvestId)
                        .toList(),
                OffsetDateTime.now(clock)
        );

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_COMPLETED, event);
        publishNotification(
                shipment.getMandorUserId(),
                "SHIPMENT_ARRIVED",
                shipment.getId(),
                "Shipment has arrived at destination"
        );
    }

    public void publishMandorApproved(Shipment shipment) {
        if (!eventsEnabled) {
            return;
        }
        ShipmentPayrollEvent event = payrollEvent(shipment, shipment.getSupirUserId(), ROLE_SUPIR, shipment.getTotalKg());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_APPROVED_BY_MANDOR, event);
        publishNotification(
                shipment.getSupirUserId(),
                "SHIPMENT_APPROVED_BY_MANDOR",
                shipment.getId(),
                "Shipment was approved by Mandor"
        );
    }

    public void publishMandorRejected(Shipment shipment) {
        if (!eventsEnabled) {
            return;
        }
        publishNotification(
                shipment.getSupirUserId(),
                "SHIPMENT_REJECTED_BY_MANDOR",
                shipment.getId(),
                "Shipment was rejected by Mandor"
        );
    }

    public void publishAdminApproved(Shipment shipment) {
        if (!eventsEnabled) {
            return;
        }
        ShipmentPayrollEvent event = payrollEvent(
                shipment,
                shipment.getMandorUserId(),
                ROLE_MANDOR,
                shipment.getKgAccepted()
        );
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_APPROVED_BY_ADMIN, event);
    }

    public void publishAdminRejected(Shipment shipment) {
        if (!eventsEnabled) {
            return;
        }
        publishNotification(
                shipment.getMandorUserId(),
                "SHIPMENT_REJECTED_BY_ADMIN",
                shipment.getId(),
                "Shipment was rejected by Admin"
        );
    }

    private ShipmentPayrollEvent payrollEvent(Shipment shipment, java.util.UUID employeeId, String employeeRole, Double kg) {
        return new ShipmentPayrollEvent(
                shipment.getId() + ":" + employeeRole,
                shipment.getId(),
                employeeId,
                employeeRole,
                kg,
                shipment.getItems().stream()
                        .map(ShipmentItem::getHarvestId)
                        .toList(),
                OffsetDateTime.now(clock)
        );
    }

    private void publishNotification(java.util.UUID recipientId, String type, java.util.UUID referenceId, String message) {
        NotificationEvent event = new NotificationEvent(
                referenceId + ":" + type,
                recipientId,
                type,
                referenceId,
                message,
                OffsetDateTime.now(clock)
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, ROUTING_KEY_NOTIFICATION_CREATED, event);
    }
}

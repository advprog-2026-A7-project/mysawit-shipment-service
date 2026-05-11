package com.mysawit.shipment.event;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.ShipmentItem;

class ShipmentEventPublisherTest {

    private static final UUID SHIPMENT_ID = UUID.fromString("abababab-abab-abab-abab-abababababab");
    private static final UUID DRIVER_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final UUID MANDOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID HARVEST_A = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID HARVEST_B = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant COMPLETED_AT = Instant.parse("2026-04-14T11:45:00Z");

    private RabbitTemplate rabbitTemplate;
    private ShipmentEventPublisher shipmentEventPublisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        shipmentEventPublisher = new ShipmentEventPublisher(
                rabbitTemplate,
                Clock.fixed(COMPLETED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void publishShipmentCompletedSendsExpectedPayloadToRabbitMq() {
        Shipment shipment = completedShipment();

        shipmentEventPublisher.publishShipmentCompleted(shipment);

        ArgumentCaptor<ShipmentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentCompletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("shipment.exchange"),
                eq("shipment.completed"),
                eventCaptor.capture()
        );

        ShipmentCompletedEvent event = eventCaptor.getValue();
        assertEquals(SHIPMENT_ID, event.shipmentId());
        assertEquals(DRIVER_ID, event.driverId());
        assertEquals(MANDOR_ID, event.mandorId());
        assertEquals(320.0, event.totalKg());
        assertEquals(List.of(HARVEST_A, HARVEST_B), event.harvestIds());
        assertEquals(OffsetDateTime.ofInstant(COMPLETED_AT, ZoneOffset.UTC), event.completedAt());
    }

    @Test
    void defaultConstructorPublishesUsingSystemClock() {
        ShipmentEventPublisher defaultClockPublisher = new ShipmentEventPublisher(rabbitTemplate);
        Shipment shipment = completedShipment();

        defaultClockPublisher.publishShipmentCompleted(shipment);

        ArgumentCaptor<ShipmentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentCompletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("shipment.exchange"),
                eq("shipment.completed"),
                eventCaptor.capture()
        );

        assertNotNull(eventCaptor.getValue().completedAt());
    }

    private Shipment completedShipment() {
        Shipment shipment = new Shipment();
        shipment.setId(SHIPMENT_ID);
        shipment.setSupirUserId(DRIVER_ID);
        shipment.setMandorUserId(MANDOR_ID);
        shipment.setTotalKg(320.0);
        shipment.setStatus(ShipmentStatus.TIBA);
        shipment.getItems().add(shipmentItem(shipment, HARVEST_A, 200.0));
        shipment.getItems().add(shipmentItem(shipment, HARVEST_B, 120.0));
        return shipment;
    }

    private ShipmentItem shipmentItem(Shipment shipment, UUID harvestId, double weightKg) {
        ShipmentItem item = new ShipmentItem();
        item.setShipment(shipment);
        item.setHarvestId(harvestId);
        item.setWeightKg(weightKg);
        return item;
    }
}

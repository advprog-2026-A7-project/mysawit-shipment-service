package com.mysawit.shipment.event;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private static final String SHIPMENT_EXCHANGE = "shipment.exchange";
    private static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    private static final String NOTIFICATION_CREATED = "notification.created";

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
                eq(SHIPMENT_EXCHANGE),
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
    void publishMandorApprovedSendsSupirPayrollAndNotificationEvents() {
        Shipment shipment = completedShipment();

        shipmentEventPublisher.publishMandorApproved(shipment);

        ArgumentCaptor<ShipmentPayrollEvent> payrollCaptor = ArgumentCaptor.forClass(ShipmentPayrollEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(SHIPMENT_EXCHANGE),
                eq("shipment.approved-by-mandor"),
                payrollCaptor.capture()
        );
        ShipmentPayrollEvent payroll = payrollCaptor.getValue();
        assertEquals(SHIPMENT_ID + ":SUPIR", payroll.eventId());
        assertEquals(DRIVER_ID, payroll.employeeId());
        assertEquals("SUPIR", payroll.employeeRole());
        assertEquals(320.0, payroll.kg());

        ArgumentCaptor<NotificationEvent> notificationCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(NOTIFICATION_EXCHANGE),
                eq(NOTIFICATION_CREATED),
                notificationCaptor.capture()
        );
        assertEquals(DRIVER_ID, notificationCaptor.getValue().recipientId());
    }

    @Test
    void publishMandorRejectedSendsSupirNotification() {
        Shipment shipment = completedShipment();

        shipmentEventPublisher.publishMandorRejected(shipment);

        ArgumentCaptor<NotificationEvent> notificationCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(NOTIFICATION_EXCHANGE),
                eq(NOTIFICATION_CREATED),
                notificationCaptor.capture()
        );
        assertEquals("SHIPMENT_REJECTED_BY_MANDOR", notificationCaptor.getValue().type());
    }

    @Test
    void publishAdminApprovedSendsMandorPayrollEvent() {
        Shipment shipment = completedShipment();
        shipment.setKgAccepted(280.0);

        shipmentEventPublisher.publishAdminApproved(shipment);

        ArgumentCaptor<ShipmentPayrollEvent> payrollCaptor = ArgumentCaptor.forClass(ShipmentPayrollEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(SHIPMENT_EXCHANGE),
                eq("shipment.approved-by-admin"),
                payrollCaptor.capture()
        );
        ShipmentPayrollEvent payroll = payrollCaptor.getValue();
        assertEquals(MANDOR_ID, payroll.employeeId());
        assertEquals("MANDOR", payroll.employeeRole());
        assertEquals(280.0, payroll.kg());
    }

    @Test
    void publishAdminRejectedSendsMandorNotification() {
        Shipment shipment = completedShipment();

        shipmentEventPublisher.publishAdminRejected(shipment);

        ArgumentCaptor<NotificationEvent> notificationCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(NOTIFICATION_EXCHANGE),
                eq(NOTIFICATION_CREATED),
                notificationCaptor.capture()
        );
        assertEquals(MANDOR_ID, notificationCaptor.getValue().recipientId());
        assertEquals("SHIPMENT_REJECTED_BY_ADMIN", notificationCaptor.getValue().type());
    }

    @Test
    void defaultConstructorPublishesUsingSystemClock() {
        ShipmentEventPublisher defaultClockPublisher = new ShipmentEventPublisher(rabbitTemplate);
        Shipment shipment = completedShipment();

        defaultClockPublisher.publishShipmentCompleted(shipment);

        ArgumentCaptor<ShipmentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentCompletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(SHIPMENT_EXCHANGE),
                eq("shipment.completed"),
                eventCaptor.capture()
        );

        assertNotNull(eventCaptor.getValue().completedAt());
    }

    @Test
    void springInjectionConstructorIsExplicitlyAutowired() throws NoSuchMethodException {
        Constructor<ShipmentEventPublisher> constructor =
                ShipmentEventPublisher.class.getConstructor(RabbitTemplate.class);

        assertTrue(constructor.isAnnotationPresent(Autowired.class));
    }

    @Test
    void publishMethodsDoNothingWhenEventsAreDisabled() {
        Shipment shipment = completedShipment();
        ReflectionTestUtils.setField(shipmentEventPublisher, "eventsEnabled", false);

        shipmentEventPublisher.publishShipmentCompleted(shipment);
        shipmentEventPublisher.publishMandorApproved(shipment);
        shipmentEventPublisher.publishMandorRejected(shipment);
        shipmentEventPublisher.publishAdminApproved(shipment);
        shipmentEventPublisher.publishAdminRejected(shipment);

        verifyNoInteractions(rabbitTemplate);
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

package com.mysawit.shipment.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentReplicaSchemaInitializer;
import com.mysawit.shipment.service.ShipmentService;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:repositorydb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "shipment.events.enabled=false",
        "spring.sql.init.mode=never"
})
class ShipmentRepositoryTest {

    private static final UUID MANDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_MANDOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SUPIR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_SUPIR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private ShipmentRepository shipmentRepository;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private ShipmentReplicaSchemaInitializer schemaInitializer;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
    }

    @Test
    void findWithFiltersSupportsLegacyFilterArguments() {
        Shipment matching = shipment(MANDOR_ID, SUPIR_ID, "Mandor Alpha", "Supir Alpha", ShipmentStatus.MEMUAT);
        Shipment other = shipment(OTHER_MANDOR_ID, OTHER_SUPIR_ID, "Mandor Beta", "Supir Beta", ShipmentStatus.TIBA);
        shipmentRepository.saveAll(List.of(matching, other));

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now().plusDays(1);

        List<Shipment> result = shipmentRepository.findWithFilters(
                SUPIR_ID.toString(),
                MANDOR_ID.toString(),
                ShipmentStatus.MEMUAT.name(),
                "alpha",
                "SUPIR",
                from,
                to
        );

        assertEquals(1, result.size());
        assertEquals(SUPIR_ID, result.get(0).getSupirUserId());
    }

    @Test
    void createdBetweenSupportsOpenEndedWindows() {
        shipmentRepository.save(shipment(MANDOR_ID, SUPIR_ID, "Mandor Alpha", "Supir Alpha", ShipmentStatus.MEMUAT));

        List<Shipment> fromOnly = shipmentRepository.findAll(
                ShipmentSpecifications.createdBetween(OffsetDateTime.now().minusDays(1), null)
        );
        List<Shipment> toOnly = shipmentRepository.findAll(
                ShipmentSpecifications.createdBetween(null, OffsetDateTime.now().plusDays(1))
        );

        assertFalse(fromOnly.isEmpty());
        assertFalse(toOnly.isEmpty());
    }

    private Shipment shipment(
            UUID mandorUserId,
            UUID supirUserId,
            String mandorName,
            String supirName,
            ShipmentStatus status
    ) {
        Shipment shipment = new Shipment();
        shipment.setMandorUserId(mandorUserId);
        shipment.setSupirUserId(supirUserId);
        shipment.setMandorName(mandorName);
        shipment.setSupirName(supirName);
        shipment.setPlantationId("plantation-1");
        shipment.setDestination("Factory");
        shipment.setTotalKg(100.0);
        shipment.setStatus(status);
        return shipment;
    }
}

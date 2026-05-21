package com.mysawit.shipment.profiling;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.event.HarvestEvent;
import com.mysawit.shipment.event.HarvestEventConsumer;
import com.mysawit.shipment.event.PlantationAssignmentEvent;
import com.mysawit.shipment.event.PlantationAssignmentEventListener;
import com.mysawit.shipment.event.ShipmentEventPublisher;
import com.mysawit.shipment.event.UserAssignmentEvent;
import com.mysawit.shipment.event.UserAssignmentEventConsumer;
import com.mysawit.shipment.event.UserDeletedEvent;
import com.mysawit.shipment.event.UserDeletedEventConsumer;
import com.mysawit.shipment.event.UserRegisteredEvent;
import com.mysawit.shipment.event.UserRegisteredEventConsumer;
import com.mysawit.shipment.event.UserUpdatedEvent;
import com.mysawit.shipment.event.UserUpdatedEventConsumer;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.ShipmentItem;
import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.repository.ShipmentRepository;
import com.mysawit.shipment.repository.WorkerPlantationAssignmentRepository;
import com.mysawit.shipment.security.JwtFixture;
import com.mysawit.shipment.service.HarvestReplicaService;
import com.mysawit.shipment.service.ShipmentReplicaSchemaInitializer;
import com.mysawit.shipment.service.UserReplicaService;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:featureprofiledb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.rabbitmq.addresses=amqp://guest:guest@localhost:5672/",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "shipment.events.enabled=true",
        "jwt.secret=" + JwtFixture.TEST_SECRET,
        "cors.allowed-origins=*",
        "spring.sql.init.mode=never"
})
@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.CouplingBetweenObjects",
        "PMD.ExcessiveImports",
        "PMD.TooManyMethods"
})
class ShipmentFeatureProfilingTest {

    private static final int EXPECTED_PROFILED_FEATURES = 24;
    private static final String PLANTATION_ID = "plantation-profile";
    private static final UUID MANDOR_ID = uuid("mandor");
    private static final UUID SUPIR_ID = uuid("supir");
    private static final UUID OTHER_SUPIR_ID = uuid("other-supir");
    private static final UUID ADMIN_ID = uuid("admin");
    private static final Path REPORT_DIR = Path.of("build/reports/profiling");

    @LocalServerPort
    private int port;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private WorkerPlantationAssignmentRepository assignmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HarvestEventConsumer harvestEventConsumer;

    @Autowired
    private UserRegisteredEventConsumer userRegisteredEventConsumer;

    @Autowired
    private UserAssignmentEventConsumer userAssignmentEventConsumer;

    @Autowired
    private UserUpdatedEventConsumer userUpdatedEventConsumer;

    @Autowired
    private UserDeletedEventConsumer userDeletedEventConsumer;

    @Autowired
    private PlantationAssignmentEventListener plantationAssignmentEventListener;

    @Autowired
    private ShipmentEventPublisher shipmentEventPublisher;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private HarvestReplicaService harvestReplicaService;

    @MockBean
    private UserReplicaService userReplicaService;

    @MockBean
    private ShipmentReplicaSchemaInitializer schemaInitializer;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        reset(rabbitTemplate, harvestReplicaService, userReplicaService);
        clearDatabase();
        seedAssignments();
    }

    @Test
    void profileAllShipmentFeatures_writesReport() {
        List<ShipmentFeatureProfileResult> results = new ArrayList<>();
        Shipment readShipment = seedShipment("read-supir", ShipmentStatus.MEMUAT, SUPIR_ID);
        seedShipment("read-admin", ShipmentStatus.MANDOR_APPROVED, OTHER_SUPIR_ID);

        results.add(profile("Health", "Shipment health endpoint", "GET /api/shipments/health", () -> given()
                .when().get("/api/shipments/health")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo("UP"))));
        results.add(profile("Health", "Actuator health endpoint", "GET /actuator/health", () -> given()
                .when().get("/actuator/health")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo("UP"))));
        results.add(profile("Security", "Missing JWT rejection", "GET /api/shipments", () -> given()
                .when().get("/api/shipments")
                .then().statusCode(HttpStatus.UNAUTHORIZED.value())));
        results.add(profile("Security", "Wrong role rejection", "GET /api/shipments", () -> given()
                .header("Authorization", bearer(JwtFixture.tokenWithRole(ADMIN_ID.toString(), "BURUH")))
                .when().get("/api/shipments")
                .then().statusCode(HttpStatus.FORBIDDEN.value())));
        results.add(profile("Read API", "SUPIR shipment list", "GET /api/shipments?status=MEMUAT", () -> given()
                .header("Authorization", bearer(JwtFixture.supirToken(SUPIR_ID.toString())))
                .queryParam("status", ShipmentStatus.MEMUAT.name())
                .when().get("/api/shipments")
                .then().statusCode(HttpStatus.OK.value())
                .body("size()", greaterThanOrEqualTo(1))));
        results.add(profile("Read API", "MANDOR shipment list", "GET /api/shipments?supirUserId&status", () -> given()
                .header("Authorization", bearer(JwtFixture.mandorToken(MANDOR_ID.toString())))
                .queryParam("supirUserId", SUPIR_ID.toString())
                .queryParam("status", ShipmentStatus.MEMUAT.name())
                .when().get("/api/shipments")
                .then().statusCode(HttpStatus.OK.value())
                .body("size()", greaterThanOrEqualTo(1))));
        results.add(profile("Read API", "ADMIN shipment list", "GET /api/shipments", () -> given()
                .header("Authorization", bearer(JwtFixture.adminToken(ADMIN_ID.toString())))
                .when().get("/api/shipments")
                .then().statusCode(HttpStatus.OK.value())
                .body("size()", greaterThanOrEqualTo(1))));
        results.add(profile("Read API", "Shipment detail", "GET /api/shipments/{id}", () -> given()
                .header("Authorization", bearer(JwtFixture.adminToken(ADMIN_ID.toString())))
                .when().get("/api/shipments/" + readShipment.getId())
                .then().statusCode(HttpStatus.OK.value())
                .body("id", equalTo(readShipment.getId().toString()))));
        results.add(profile("Read API", "Available supirs", "GET /api/shipments/available-supirs", () -> given()
                .header("Authorization", bearer(JwtFixture.mandorToken(MANDOR_ID.toString())))
                .queryParam("name", "Profile")
                .when().get("/api/shipments/available-supirs")
                .then().statusCode(HttpStatus.OK.value())
                .body("size()", greaterThanOrEqualTo(1))));

        results.add(profile("Write API", "Create shipment", "POST /api/shipments", this::profileCreateShipment));
        results.add(profile("Write API", "SUPIR status MEMUAT to MENGIRIM", "PATCH /api/shipments/{id}/status",
                this::profileStatusToMengirim));
        results.add(profile("Write API", "SUPIR status MENGIRIM to TIBA", "PATCH /api/shipments/{id}/status",
                this::profileStatusToTiba));
        results.add(profile("Write API", "Mandor approve shipment", "PATCH /api/shipments/{id}/mandor-approval",
                this::profileMandorApproval));
        results.add(profile("Write API", "Mandor reject shipment", "PATCH /api/shipments/{id}/mandor-approval",
                this::profileMandorRejection));
        results.add(profile("Write API", "Admin approve shipment", "PATCH /api/shipments/{id}/admin-approval",
                this::profileAdminApproval));
        results.add(profile("Write API", "Admin partial reject shipment", "PATCH /api/shipments/{id}/admin-approval",
                this::profileAdminPartialRejection));
        results.add(profile("Write API", "Admin reject shipment", "PATCH /api/shipments/{id}/admin-approval",
                this::profileAdminRejection));

        results.add(profile("Events", "Harvest replica consumer", "HarvestEventConsumer", this::profileHarvestConsumer));
        results.add(profile("Events", "User registered consumer", "UserRegisteredEventConsumer",
                this::profileUserRegisteredConsumer));
        results.add(profile("Events", "User assignment consumer", "UserAssignmentEventConsumer",
                this::profileUserAssignmentConsumer));
        results.add(profile("Events", "User updated consumer", "UserUpdatedEventConsumer",
                this::profileUserUpdatedConsumer));
        results.add(profile("Events", "User deleted consumer", "UserDeletedEventConsumer",
                this::profileUserDeletedConsumer));
        results.add(profile("Events", "Worker assignment listener", "PlantationAssignmentEventListener",
                this::profilePlantationAssignmentListener));
        results.add(profile("Events", "Shipment outbound publisher", "ShipmentEventPublisher",
                this::profileShipmentOutboundPublisher));

        ShipmentFeatureProfileReportWriter writer = new ShipmentFeatureProfileReportWriter(REPORT_DIR);
        Path markdownReport = writer.write(results);

        assertEquals(EXPECTED_PROFILED_FEATURES, results.size());
        assertTrue(Files.exists(markdownReport));
        assertTrue(Files.exists(writer.htmlReportPath()));
        assertTrue(Files.exists(writer.pngReportPath()));
        assertTrue(results.stream().allMatch(ShipmentFeatureProfileResult::success), failedScenarios(results));
    }

    private ShipmentFeatureProfileResult profile(
            String area,
            String feature,
            String entryPoint,
            ProfileAction action
    ) {
        long start = System.nanoTime();
        try {
            action.run();
            return new ShipmentFeatureProfileResult(area, feature, entryPoint, true, elapsedMs(start), "OK");
        } catch (RuntimeException | AssertionError ex) {
            return new ShipmentFeatureProfileResult(
                    area,
                    feature,
                    entryPoint,
                    false,
                    elapsedMs(start),
                    ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    private void profileCreateShipment() {
        UUID harvestId = uuid("create-shipment-approved-harvest");
        when(harvestReplicaService.getHarvestById(MANDOR_ID, harvestId))
                .thenReturn(new HarvestReplicaService.HarvestDetails(
                        harvestId,
                        MANDOR_ID,
                        PLANTATION_ID,
                        "Approved",
                        95.0
                ));
        given()
                .header("Authorization", bearer(JwtFixture.mandorToken(MANDOR_ID.toString())))
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "supirUserId": "%s",
                          "destination": "Pabrik Profiling",
                          "items": [{"harvestId": "%s", "weightKg": 95.0}]
                        }
                        """.formatted(SUPIR_ID, harvestId))
                .when().post("/api/shipments")
                .then().statusCode(HttpStatus.CREATED.value())
                .body("status", equalTo(ShipmentStatus.MEMUAT.name()));
    }

    private void profileStatusToMengirim() {
        Shipment shipment = seedShipment("status-mengirim", ShipmentStatus.MEMUAT, SUPIR_ID);
        given()
                .header("Authorization", bearer(JwtFixture.supirToken(SUPIR_ID.toString())))
                .contentType(ContentType.JSON)
                .body("{\"status\": \"MENGIRIM\"}")
                .when().patch("/api/shipments/" + shipment.getId() + "/status")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo(ShipmentStatus.MENGIRIM.name()));
    }

    private void profileStatusToTiba() {
        Shipment shipment = seedShipment("status-tiba", ShipmentStatus.MENGIRIM, SUPIR_ID);
        given()
                .header("Authorization", bearer(JwtFixture.supirToken(SUPIR_ID.toString())))
                .contentType(ContentType.JSON)
                .body("{\"status\": \"TIBA\"}")
                .when().patch("/api/shipments/" + shipment.getId() + "/status")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo(ShipmentStatus.TIBA.name()));
    }

    private void profileMandorApproval() {
        Shipment shipment = seedShipment("mandor-approve", ShipmentStatus.TIBA, SUPIR_ID);
        given()
                .header("Authorization", bearer(JwtFixture.mandorToken(MANDOR_ID.toString())))
                .contentType(ContentType.JSON)
                .body("{\"status\": \"MANDOR_APPROVED\"}")
                .when().patch("/api/shipments/" + shipment.getId() + "/mandor-approval")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo(ShipmentStatus.MANDOR_APPROVED.name()));
    }

    private void profileMandorRejection() {
        Shipment shipment = seedShipment("mandor-reject", ShipmentStatus.TIBA, SUPIR_ID);
        given()
                .header("Authorization", bearer(JwtFixture.mandorToken(MANDOR_ID.toString())))
                .contentType(ContentType.JSON)
                .body("{\"status\": \"MANDOR_REJECTED\", \"rejectionReason\": \"profiling reject\"}")
                .when().patch("/api/shipments/" + shipment.getId() + "/mandor-approval")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo(ShipmentStatus.MANDOR_REJECTED.name()));
    }

    private void profileAdminApproval() {
        Shipment shipment = seedShipment("admin-approve", ShipmentStatus.MANDOR_APPROVED, SUPIR_ID);
        given()
                .header("Authorization", bearer(JwtFixture.adminToken(ADMIN_ID.toString())))
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ADMIN_APPROVED\"}")
                .when().patch("/api/shipments/" + shipment.getId() + "/admin-approval")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo(ShipmentStatus.ADMIN_APPROVED.name()));
    }

    private void profileAdminPartialRejection() {
        Shipment shipment = seedShipment("admin-partial", ShipmentStatus.MANDOR_APPROVED, SUPIR_ID);
        given()
                .header("Authorization", bearer(JwtFixture.adminToken(ADMIN_ID.toString())))
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "status": "PARTIALLY_REJECTED",
                          "rejectionReason": "profiling partial",
                          "kgAccepted": 50.0
                        }
                        """)
                .when().patch("/api/shipments/" + shipment.getId() + "/admin-approval")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo(ShipmentStatus.PARTIALLY_REJECTED.name()));
    }

    private void profileAdminRejection() {
        Shipment shipment = seedShipment("admin-reject", ShipmentStatus.MANDOR_APPROVED, SUPIR_ID);
        given()
                .header("Authorization", bearer(JwtFixture.adminToken(ADMIN_ID.toString())))
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ADMIN_REJECTED\", \"rejectionReason\": \"profiling reject\"}")
                .when().patch("/api/shipments/" + shipment.getId() + "/admin-approval")
                .then().statusCode(HttpStatus.OK.value())
                .body("status", equalTo(ShipmentStatus.ADMIN_REJECTED.name()));
    }

    private void profileHarvestConsumer() {
        harvestEventConsumer.onHarvestEvent(harvestEvent("consumer-harvest", uuid("consumer-harvest"), 77.0));
        verify(harvestReplicaService).upsertHarvest(any(HarvestEvent.class));
    }

    private void profileUserRegisteredConsumer() {
        UUID userId = uuid("consumer-user-registered");
        userRegisteredEventConsumer.onUserRegistered(new UserRegisteredEvent(
                userId.toString(),
                "profile-user@example.test",
                "SUPIR",
                "profile-user"
        ));
        verify(userReplicaService).upsertFromRegistration(any(UserRegisteredEvent.class));
    }

    private void profileUserAssignmentConsumer() {
        UUID userId = uuid("consumer-user-assignment");
        userAssignmentEventConsumer.onUserAssignment(new UserAssignmentEvent(
                userId.toString(),
                MANDOR_ID.toString(),
                "Mandor Profile",
                UserAssignmentEvent.AssignmentAction.ASSIGNED,
                Instant.now()
        ));
        verify(userReplicaService).applyAssignment(any(UserAssignmentEvent.class));
    }

    private void profileUserUpdatedConsumer() {
        UUID userId = uuid("consumer-user-updated");
        userUpdatedEventConsumer.onUserUpdated(new UserUpdatedEvent(
                userId.toString(),
                "updated-profile@example.test",
                "MANDOR",
                "updated-profile",
                "Updated Profile",
                Instant.now()
        ));
        verify(userReplicaService).upsertFromUpdate(any(UserUpdatedEvent.class));
    }

    private void profileUserDeletedConsumer() {
        UUID userId = uuid("consumer-user-deleted");
        userRegisteredEventConsumer.onUserRegistered(new UserRegisteredEvent(
                userId.toString(),
                "deleted-profile@example.test",
                "SUPIR",
                "deleted-profile"
        ));
        userDeletedEventConsumer.onUserDeleted(new UserDeletedEvent(
                userId.toString(),
                "SUPIR",
                MANDOR_ID.toString(),
                Instant.now()
        ));
        verify(userReplicaService).markDeleted(any(UserDeletedEvent.class));
    }

    private void profilePlantationAssignmentListener() {
        plantationAssignmentEventListener.handleAssignmentEvent(plantationEvent(
                "consumer-plantation-listener",
                uuid("consumer-plantation-listener"),
                "SUPIR"
        ));
    }

    private void profileShipmentOutboundPublisher() {
        Shipment shipment = seedShipment("publisher", ShipmentStatus.MANDOR_APPROVED, SUPIR_ID);
        shipment.setKgAccepted(80.0);
        shipmentEventPublisher.publishShipmentCompleted(shipment);
        shipmentEventPublisher.publishMandorApproved(shipment);
        shipmentEventPublisher.publishMandorRejected(shipment);
        shipmentEventPublisher.publishAdminApproved(shipment);
        shipmentEventPublisher.publishAdminRejected(shipment);
    }

    private void clearDatabase() {
        jdbcTemplate.execute("delete from shipment_items");
        shipmentRepository.deleteAll();
        assignmentRepository.deleteAll();
    }

    private void seedAssignments() {
        assignmentRepository.save(assignment(MANDOR_ID, "MANDOR", "Mandor Profile"));
        assignmentRepository.save(assignment(SUPIR_ID, "SUPIR", "Supir Profile"));
        assignmentRepository.save(assignment(OTHER_SUPIR_ID, "SUPIR", "Other Supir Profile"));
        assignmentRepository.flush();
    }

    private WorkerPlantationAssignment assignment(UUID userId, String role, String name) {
        WorkerPlantationAssignment assignment = new WorkerPlantationAssignment();
        assignment.setUserId(userId);
        assignment.setRole(role);
        assignment.setName(name);
        assignment.setPlantationId(PLANTATION_ID);
        assignment.setLastEventId("profile-" + userId);
        assignment.setUpdatedAt(OffsetDateTime.now());
        return assignment;
    }

    private Shipment seedShipment(String key, ShipmentStatus status, UUID supirUserId) {
        Shipment shipment = detachedShipment(key, status);
        shipment.setSupirUserId(supirUserId);
        shipment.setSupirName(supirUserId.equals(SUPIR_ID) ? "Supir Profile" : "Other Supir Profile");
        return shipmentRepository.saveAndFlush(shipment);
    }

    private Shipment detachedShipment(String key, ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setMandorUserId(MANDOR_ID);
        shipment.setMandorName("Mandor Profile");
        shipment.setSupirUserId(SUPIR_ID);
        shipment.setSupirName("Supir Profile");
        shipment.setPlantationId(PLANTATION_ID);
        shipment.setDestination("Pabrik Profiling");
        shipment.setTotalKg(100.0);
        shipment.setStatus(status);
        if (ShipmentStatus.MANDOR_APPROVED.equals(status)) {
            shipment.setMandorReviewedAt(OffsetDateTime.now());
        }
        if (ShipmentStatus.ADMIN_APPROVED.equals(status)) {
            shipment.setKgAccepted(100.0);
            shipment.setAdminReviewedAt(OffsetDateTime.now());
        }
        ShipmentItem item = new ShipmentItem();
        item.setHarvestId(uuid(key + "-harvest"));
        item.setWeightKg(100.0);
        shipment.addItem(item);
        return shipment;
    }

    private HarvestEvent harvestEvent(String key, UUID harvestId, double weightKg) {
        return new HarvestEvent(
                "profile-" + key,
                harvestId,
                uuid(key + "-harvester"),
                MANDOR_ID,
                PLANTATION_ID,
                weightKg,
                "APPROVED",
                OffsetDateTime.now()
        );
    }

    private PlantationAssignmentEvent plantationEvent(String key, UUID userId, String role) {
        return new PlantationAssignmentEvent(
                "profile-" + key,
                userId,
                "Worker " + key,
                role,
                PLANTATION_ID,
                PlantationAssignmentEvent.AssignmentAction.ASSIGNED,
                OffsetDateTime.now()
        );
    }

    private String failedScenarios(List<ShipmentFeatureProfileResult> results) {
        return results.stream()
                .filter(result -> !result.success())
                .map(result -> result.feature() + " -> " + result.notes())
                .collect(Collectors.joining("\n"));
    }

    private long elapsedMs(long startNanos) {
        long elapsed = System.nanoTime() - startNanos;
        return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(elapsed));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private static UUID uuid(String seed) {
        return UUID.nameUUIDFromBytes(("shipment-feature-profile-" + seed).getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface ProfileAction {
        void run();
    }
}

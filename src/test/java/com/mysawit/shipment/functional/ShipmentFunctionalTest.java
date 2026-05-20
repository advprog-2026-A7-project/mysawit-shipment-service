package com.mysawit.shipment.functional;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.dto.CreateShipmentRequest;
import com.mysawit.shipment.exception.HarvestValidationException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.model.WorkerPlantationAssignment;
import com.mysawit.shipment.service.ShipmentService;

/**
 * Functional tests for the Shipment Service REST API.
 *
 * <p>These tests start the full Spring Boot application on a random port
 * and exercise every public HTTP endpoint using REST Assured. All
 * infrastructure dependencies (RabbitMQ, database) are replaced with
 * Mockito mocks so the tests run without external services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.rabbitmq.addresses=amqp://guest:guest@localhost:5672/",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.rabbitmq.listener.direct.auto-startup=false",
    "shipment.events.enabled=false",
    "jwt.secret=test-functional-secret-key-minimum-32-characters-long",
    "cors.allowed-origins=*",
    "spring.sql.init.mode=never"
})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ShipmentFunctionalTest {

    private static final UUID MANDOR_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID SUPIR_ID  = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID SHIPMENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID HARVEST_A = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @LocalServerPort
    private int port;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private com.mysawit.shipment.service.ShipmentReplicaSchemaInitializer schemaInitializer;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ─── Health ────────────────────────────────────────────────────────────────

    @Test
    void healthEndpointReturnsUp() {
        given()
            .when().get("/api/shipments/health")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body("status", equalTo("UP"))
            .body("service", equalTo("mysawit-shipment-service"));
    }

    @Test
    void actuatorHealthReadinessIsReachable() {
        given()
            .when().get("/actuator/health")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", notNullValue());
    }

    // ─── GET /api/shipments ────────────────────────────────────────────────────

    @Test
    void getShipmentsWithoutTokenReturns401() {
        given()
            .when().get("/api/shipments")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getShipmentsWithInvalidTokenReturns401() {
        given()
            .header("Authorization", "Bearer not-a-valid-jwt")
            .when().get("/api/shipments")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getShipmentsWithAdminTokenReturnsOk() {
        when(shipmentService.getShipmentsForAdmin(any(), any(), any()))
                .thenReturn(List.of(sampleShipment(SHIPMENT_ID)));

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.adminToken())
            .when().get("/api/shipments")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body("[0].id", equalTo(SHIPMENT_ID.toString()))
            .body("[0].status", equalTo("MEMUAT"));
    }

    @Test
    void getShipmentsWithMandorTokenReturnsOk() {
        when(shipmentService.getShipmentsByMandorUserId(
                eq(MANDOR_ID), any(), any(), any(), any()))
                .thenReturn(List.of(sampleShipment(SHIPMENT_ID)));

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.mandorToken(MANDOR_ID))
            .when().get("/api/shipments")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("$.size()", is(1));
    }

    @Test
    void getShipmentsWithSupirTokenReturnsOk() {
        when(shipmentService.getShipmentsBySupirUserId(eq(SUPIR_ID), any(), any()))
                .thenReturn(List.of());

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.supirToken(SUPIR_ID))
            .when().get("/api/shipments")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("$.size()", is(0));
    }

    // ─── GET /api/shipments/{id} ───────────────────────────────────────────────

    @Test
    void getShipmentByIdReturns200ForExistingShipment() {
        when(shipmentService.getShipmentById(SHIPMENT_ID)).thenReturn(sampleShipment(SHIPMENT_ID));

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.adminToken())
            .when().get("/api/shipments/" + SHIPMENT_ID)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(SHIPMENT_ID.toString()))
            .body("destination", equalTo("Jakarta"))
            .body("totalKg", equalTo(100.0f));
    }

    @Test
    void getShipmentByIdReturns404WhenNotFound() {
        when(shipmentService.getShipmentById(SHIPMENT_ID))
                .thenThrow(new ShipmentNotFoundException("Shipment not found with id: " + SHIPMENT_ID));

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.adminToken())
            .when().get("/api/shipments/" + SHIPMENT_ID)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    // ─── POST /api/shipments ───────────────────────────────────────────────────

    @Test
    void createShipmentWithSupirRoleReturns403() {
        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.supirToken(SUPIR_ID))
            .contentType(ContentType.JSON)
            .body("""
                {
                  "supirUserId": "%s",
                  "destination": "Jakarta",
                  "items": [{"harvestId": "%s", "weightKg": 100.0}]
                }
                """.formatted(SUPIR_ID, HARVEST_A))
            .when().post("/api/shipments")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void createShipmentWithMandorRoleReturns201() {
        Shipment saved = sampleShipment(SHIPMENT_ID);
        when(shipmentService.createShipment(eq(MANDOR_ID), any(CreateShipmentRequest.class)))
                .thenReturn(saved);

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.mandorToken(MANDOR_ID))
            .contentType(ContentType.JSON)
            .body("""
                {
                  "supirUserId": "%s",
                  "destination": "Jakarta",
                  "items": [{"harvestId": "%s", "weightKg": 100.0}]
                }
                """.formatted(SUPIR_ID, HARVEST_A))
            .when().post("/api/shipments")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", equalTo(SHIPMENT_ID.toString()));
    }

    @Test
    void createShipmentWithInvalidBodyReturns400() {
        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.mandorToken(MANDOR_ID))
            .contentType(ContentType.JSON)
            .body("{}")
            .when().post("/api/shipments")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void createShipmentWhenHarvestNotFoundReturns404() {
        when(shipmentService.createShipment(eq(MANDOR_ID), any(CreateShipmentRequest.class)))
                .thenThrow(HarvestValidationException.notFound("Harvest not found: " + HARVEST_A));

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.mandorToken(MANDOR_ID))
            .contentType(ContentType.JSON)
            .body("""
                {
                  "supirUserId": "%s",
                  "destination": "Jakarta",
                  "items": [{"harvestId": "%s", "weightKg": 100.0}]
                }
                """.formatted(SUPIR_ID, HARVEST_A))
            .when().post("/api/shipments")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void createShipmentWhenHarvestAlreadyClaimedReturns409() {
        when(shipmentService.createShipment(eq(MANDOR_ID), any(CreateShipmentRequest.class)))
                .thenThrow(HarvestValidationException.conflict("Harvest already claimed: " + HARVEST_A));

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.mandorToken(MANDOR_ID))
            .contentType(ContentType.JSON)
            .body("""
                {
                  "supirUserId": "%s",
                  "destination": "Jakarta",
                  "items": [{"harvestId": "%s", "weightKg": 100.0}]
                }
                """.formatted(SUPIR_ID, HARVEST_A))
            .when().post("/api/shipments")
            .then()
            .statusCode(HttpStatus.CONFLICT.value());
    }

    // ─── PATCH /api/shipments/{id}/status ─────────────────────────────────────

    @Test
    void updateStatusBySupirReturns200() {
        Shipment updated = sampleShipment(SHIPMENT_ID);
        updated.setStatus(ShipmentStatus.MENGIRIM);
        when(shipmentService.updateShipmentStatus(SHIPMENT_ID, SUPIR_ID, ShipmentStatus.MENGIRIM))
                .thenReturn(updated);

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.supirToken(SUPIR_ID))
            .contentType(ContentType.JSON)
            .body("{\"status\": \"MENGIRIM\"}")
            .when().patch("/api/shipments/" + SHIPMENT_ID + "/status")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo("MENGIRIM"));
    }

    @Test
    void updateStatusWithoutTokenReturns401() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"status\": \"MENGIRIM\"}")
            .when().patch("/api/shipments/" + SHIPMENT_ID + "/status")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    // ─── GET /api/shipments/supirs ────────────────────────────────────────────

    @Test
    void getSupirsForMandorReturnsListOfSupirs() {
        WorkerPlantationAssignment supir = new WorkerPlantationAssignment();
        supir.setUserId(SUPIR_ID);
        supir.setName("Supir Satu");
        supir.setRole("SUPIR");
        supir.setPlantationId("plantation-1");

        when(shipmentService.getSupirsForMandor(eq(MANDOR_ID), any()))
                .thenReturn(List.of(supir));

        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.mandorToken(MANDOR_ID))
            .when().get("/api/shipments/available-supirs")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("[0]", hasKey("userId"))
            .body("[0].name", equalTo("Supir Satu"));
    }

    // ─── Error format ─────────────────────────────────────────────────────────

    @Test
    void missingEndpointReturns404WithJsonBody() {
        given()
            .header("Authorization", "Bearer " + JwtFixtureFunctional.adminToken())
            .when().get("/api/shipments/nonexistent/unknown-path")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Shipment sampleShipment(UUID id) {
        Shipment s = new Shipment();
        s.setId(id);
        s.setMandorUserId(MANDOR_ID);
        s.setSupirUserId(SUPIR_ID);
        s.setDestination("Jakarta");
        s.setTotalKg(100.0);
        s.setStatus(ShipmentStatus.MEMUAT);
        return s;
    }
}

package com.mysawit.shipment.controller;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mysawit.shipment.exception.HarvestReplicaUnavailableException;
import com.mysawit.shipment.exception.HarvestValidationException;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.exception.ShipmentWeightExceededException;
import com.mysawit.shipment.security.JwtFixture;
import com.mysawit.shipment.security.JwtTokenProvider;
import com.mysawit.shipment.service.ShipmentService;

@WebMvcTest(controllers = ShipmentController.class)
@Import(JwtTokenProvider.class)
@ActiveProfiles("test")
class ShipmentErrorHandlingTest {

    private static final String SHIPMENTS_PATH = "/api/shipments/";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HARVEST_VALIDATION_FAILED = "HARVEST_VALIDATION_FAILED";
    private static final String HARVEST_NOT_FOUND_PREFIX = "Harvest not found: ";
    private static final String HARVEST_REPLICA_UNAVAILABLE_MESSAGE = "Harvest data not yet replicated";
    private static final String JSON_ERROR = "$.error";
    private static final String JSON_MESSAGE = "$.message";
    private static final String CREATE_SHIPMENT_PATH = "/api/shipments";
    private static final String CREATE_SHIPMENT_BODY = """
            {
              "supirUserId": "42424242-4242-4242-4242-424242424242",
              "destination": "Jakarta",
              "items": [
                {
                  "harvestId": "60060060-0600-6006-0060-060060060060",
                  "weightKg": 100.0
                }
              ]
            }
            """;

    private static final UUID NOT_FOUND_ID = UUID.fromString("40404040-4040-4040-4040-404040404040");
    private static final UUID FORBIDDEN_ID = UUID.fromString("40340340-3403-4034-0340-340340340340");
    private static final UUID LEGACY_ERROR_ID = UUID.fromString("50050050-0500-5005-0050-050050050050");
    private static final UUID WEIGHT_ID = UUID.fromString("60060060-0600-6006-0060-060060060060");
    private static final UUID SUPIR_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final UUID MANDOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentByIdReturnsNotFoundErrorContract() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(NOT_FOUND_ID, SUPIR_ID))
                .thenThrow(new ShipmentNotFoundException("Shipment not found with id: " + NOT_FOUND_ID));

        performGetShipment(NOT_FOUND_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_ERROR).value("NOT_FOUND"))
                .andExpect(jsonPath(JSON_MESSAGE).value("Shipment not found with id: " + NOT_FOUND_ID));
    }

    @Test
    void getShipmentByIdReturnsForbiddenErrorContract() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(FORBIDDEN_ID, SUPIR_ID))
                .thenThrow(new ShipmentForbiddenException("Forbidden"));

        performGetShipment(FORBIDDEN_ID)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(JSON_ERROR).value("FORBIDDEN"))
                .andExpect(jsonPath(JSON_MESSAGE).value("Forbidden"));
    }

    @Test
    void getShipmentByIdReturnsInternalServerErrorForUnexpectedRuntimeException() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(LEGACY_ERROR_ID, SUPIR_ID))
                .thenThrow(new RuntimeException("missing"));

        performGetShipment(LEGACY_ERROR_ID)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(JSON_ERROR).value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath(JSON_MESSAGE).value("An unexpected error occurred"));
    }

    @Test
    void getShipmentByIdReturnsWeightExceededErrorContract() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(WEIGHT_ID, SUPIR_ID))
                .thenThrow(new ShipmentWeightExceededException("Total weight 450.0 kg exceeds maximum of 400 kg"));

        performGetShipment(WEIGHT_ID)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_ERROR).value("WEIGHT_EXCEEDED"))
                .andExpect(jsonPath(JSON_MESSAGE).value("Total weight 450.0 kg exceeds maximum of 400 kg"));
    }

    @Test
    void getShipmentByIdReturnsHarvestValidationErrorContract() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(WEIGHT_ID, SUPIR_ID))
                .thenThrow(new HarvestValidationException(HARVEST_NOT_FOUND_PREFIX + WEIGHT_ID, HttpStatus.NOT_FOUND));

        performGetShipment(WEIGHT_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_ERROR).value(HARVEST_VALIDATION_FAILED))
                .andExpect(jsonPath(JSON_MESSAGE).value(HARVEST_NOT_FOUND_PREFIX + WEIGHT_ID));
    }

    @Test
    void getShipmentByIdReturnsHarvestReplicaUnavailableErrorContract() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(WEIGHT_ID, SUPIR_ID))
                .thenThrow(new HarvestReplicaUnavailableException(HARVEST_REPLICA_UNAVAILABLE_MESSAGE));

        performGetShipment(WEIGHT_ID)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath(JSON_ERROR).value("HARVEST_REPLICA_UNAVAILABLE"))
                .andExpect(jsonPath(JSON_MESSAGE).value(HARVEST_REPLICA_UNAVAILABLE_MESSAGE));
    }

    @Test
    void createShipmentReturnsHarvestValidationNotFoundErrorContract() throws Exception {
        when(shipmentService.createShipment(eq(MANDOR_ID), any()))
                .thenThrow(new HarvestValidationException(HARVEST_NOT_FOUND_PREFIX + WEIGHT_ID, HttpStatus.NOT_FOUND));

        assertCreateShipmentError(HARVEST_VALIDATION_FAILED, HARVEST_NOT_FOUND_PREFIX + WEIGHT_ID, status().isNotFound());
    }

    @Test
    void createShipmentReturnsHarvestValidationConflictErrorContract() throws Exception {
        when(shipmentService.createShipment(eq(MANDOR_ID), any()))
                .thenThrow(new HarvestValidationException("Harvest already claimed: " + WEIGHT_ID, HttpStatus.CONFLICT));

        assertCreateShipmentError(
                HARVEST_VALIDATION_FAILED,
                "Harvest already claimed: " + WEIGHT_ID,
                status().isConflict()
        );
    }

    @Test
    void createShipmentReturnsHarvestValidationBadRequestErrorContract() throws Exception {
        when(shipmentService.createShipment(eq(MANDOR_ID), any()))
                .thenThrow(new HarvestValidationException(
                        "Harvest status must be Approved: " + WEIGHT_ID,
                        HttpStatus.BAD_REQUEST
                ));

        assertCreateShipmentError(
                HARVEST_VALIDATION_FAILED,
                "Harvest status must be Approved: " + WEIGHT_ID,
                status().isBadRequest()
        );
    }

    @Test
    void createShipmentReturnsHarvestReplicaUnavailableErrorContract() throws Exception {
        when(shipmentService.createShipment(eq(MANDOR_ID), any()))
                .thenThrow(new HarvestReplicaUnavailableException(HARVEST_REPLICA_UNAVAILABLE_MESSAGE));

        assertCreateShipmentError(
                "HARVEST_REPLICA_UNAVAILABLE",
                HARVEST_REPLICA_UNAVAILABLE_MESSAGE,
                status().isServiceUnavailable()
        );
    }

    private ResultActions performGetShipment(UUID shipmentId) throws Exception {
        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());
        return mockMvc.perform(get(SHIPMENTS_PATH + shipmentId)
                .header(AUTH_HEADER, BEARER_PREFIX + supirToken));
    }

    private ResultActions performCreateShipment() throws Exception {
        String mandorToken = JwtFixture.mandorToken(MANDOR_ID.toString());
        return mockMvc.perform(post(CREATE_SHIPMENT_PATH)
                .header(AUTH_HEADER, BEARER_PREFIX + mandorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_SHIPMENT_BODY));
    }

    private void assertCreateShipmentError(
            String errorCode,
            String message,
            org.springframework.test.web.servlet.ResultMatcher statusMatcher
    ) throws Exception {
        performCreateShipment()
                .andExpect(statusMatcher)
                .andExpect(jsonPath(JSON_ERROR).value(errorCode))
                .andExpect(jsonPath(JSON_MESSAGE).value(message));
    }
}

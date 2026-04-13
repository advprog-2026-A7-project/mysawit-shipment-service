package com.mysawit.shipment.controller;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.security.JwtFixture;
import com.mysawit.shipment.security.JwtTokenProvider;
import com.mysawit.shipment.service.ShipmentService;

@WebMvcTest(controllers = ShipmentController.class)
@Import(JwtTokenProvider.class)
@ActiveProfiles("test")
class ShipmentErrorHandlingTest {

    private static final UUID NOT_FOUND_ID = UUID.fromString("40404040-4040-4040-4040-404040404040");
    private static final UUID FORBIDDEN_ID = UUID.fromString("40340340-3403-4034-0340-340340340340");
    private static final UUID LEGACY_ERROR_ID = UUID.fromString("50050050-0500-5005-0050-050050050050");
    private static final UUID SUPIR_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentByIdReturnsNotFoundErrorContract() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(NOT_FOUND_ID, SUPIR_ID))
                .thenThrow(new ShipmentNotFoundException("Shipment not found with id: " + NOT_FOUND_ID));

        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());

        mockMvc.perform(get("/api/shipments/" + NOT_FOUND_ID)
                        .header("Authorization", "Bearer " + supirToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Shipment not found with id: " + NOT_FOUND_ID));
    }

    @Test
    void getShipmentByIdReturnsForbiddenErrorContract() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(FORBIDDEN_ID, SUPIR_ID))
                .thenThrow(new ShipmentForbiddenException("Forbidden"));

        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());

        mockMvc.perform(get("/api/shipments/" + FORBIDDEN_ID)
                        .header("Authorization", "Bearer " + supirToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void getShipmentByIdReturnsInternalServerErrorForUnexpectedRuntimeException() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(LEGACY_ERROR_ID, SUPIR_ID))
                .thenThrow(new RuntimeException("missing"));

        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());

        mockMvc.perform(get("/api/shipments/" + LEGACY_ERROR_ID)
                        .header("Authorization", "Bearer " + supirToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}

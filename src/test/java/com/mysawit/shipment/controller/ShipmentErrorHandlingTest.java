package com.mysawit.shipment.controller;

import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentErrorHandlingTest {

    private static final UUID NOT_FOUND_ID = UUID.fromString("40404040-4040-4040-4040-404040404040");
    private static final UUID FORBIDDEN_ID = UUID.fromString("40340340-3403-4034-0340-340340340340");
    private static final UUID LEGACY_ERROR_ID = UUID.fromString("50050050-0500-5005-0050-050050050050");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentByIdReturnsNotFoundErrorContract() throws Exception {
        when(shipmentService.getShipmentById(NOT_FOUND_ID))
                .thenThrow(new ShipmentNotFoundException("Shipment not found with id: " + NOT_FOUND_ID));

        mockMvc.perform(get("/api/shipments/" + NOT_FOUND_ID)
                        .header("Authorization", "Bearer token-with-supir-role"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Shipment not found with id: " + NOT_FOUND_ID));
    }

    @Test
    void getShipmentByIdReturnsForbiddenErrorContract() throws Exception {
        when(shipmentService.getShipmentById(FORBIDDEN_ID))
                .thenThrow(new ShipmentForbiddenException("Forbidden"));

        mockMvc.perform(get("/api/shipments/" + FORBIDDEN_ID)
                        .header("Authorization", "Bearer token-with-supir-role"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void getShipmentByIdReturnsLegacyRuntimeErrorContract() throws Exception {
        when(shipmentService.getShipmentById(LEGACY_ERROR_ID))
                .thenThrow(new RuntimeException("missing"));

        mockMvc.perform(get("/api/shipments/" + LEGACY_ERROR_ID)
                        .header("Authorization", "Bearer token-with-supir-role"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("missing"));
    }
}

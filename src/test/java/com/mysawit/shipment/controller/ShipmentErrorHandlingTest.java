package com.mysawit.shipment.controller;

import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentByIdReturnsNotFoundErrorContract() throws Exception {
        when(shipmentService.getShipmentById(404L))
                .thenThrow(new ShipmentNotFoundException("Shipment not found with id: 404"));

        mockMvc.perform(get("/api/shipments/404")
                        .header("Authorization", "Bearer token-with-supir-role"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Shipment not found with id: 404"));
    }

    @Test
    void getShipmentByIdReturnsForbiddenErrorContract() throws Exception {
        when(shipmentService.getShipmentById(403L))
                .thenThrow(new ShipmentForbiddenException("Forbidden"));

        mockMvc.perform(get("/api/shipments/403")
                        .header("Authorization", "Bearer token-with-supir-role"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void getShipmentByIdReturnsLegacyRuntimeErrorContract() throws Exception {
        when(shipmentService.getShipmentById(500L))
                .thenThrow(new RuntimeException("missing"));

        mockMvc.perform(get("/api/shipments/500")
                        .header("Authorization", "Bearer token-with-supir-role"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("missing"));
    }
}

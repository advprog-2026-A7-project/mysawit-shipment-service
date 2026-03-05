package com.mysawit.shipment.security;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.exception.ShipmentInvalidTransitionException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentStatusUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void patchStatusUsesSupirUserIdFromJwtClaims() throws Exception {
        Shipment updated = new Shipment();
        updated.setId(1L);
        updated.setSupirUserId(42L);
        updated.setStatus("MENGIRIM");

        when(shipmentService.updateShipmentStatus(1L, 42L, ShipmentStatus.MENGIRIM))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/shipments/1/status")
                        .header("Authorization", "Bearer token-with-supir-role-user-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MENGIRIM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MENGIRIM"));

        verify(shipmentService).updateShipmentStatus(1L, 42L, ShipmentStatus.MENGIRIM);
    }

    @Test
    void patchStatusReturnsConflictWhenTransitionInvalid() throws Exception {
        when(shipmentService.updateShipmentStatus(1L, 42L, ShipmentStatus.TIBA))
                .thenThrow(new ShipmentInvalidTransitionException("Invalid status transition"));

        mockMvc.perform(patch("/api/shipments/1/status")
                        .header("Authorization", "Bearer token-with-supir-role-user-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TIBA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Invalid status transition"));
    }
}

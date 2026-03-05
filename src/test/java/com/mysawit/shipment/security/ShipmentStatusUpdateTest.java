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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentStatusUpdateTest {

    private static final String PATCH_STATUS_PATH = "/api/shipments/1/status";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String NON_SUPIR_TOKEN = "Bearer token-with-non-supir-role";
    private static final String SUPIR_42_TOKEN = "Bearer token-with-supir-role-user-42";
    private static final String MENGIRIM_BODY = "{\"status\":\"MENGIRIM\"}";
    private static final String TIBA_BODY = "{\"status\":\"TIBA\"}";
    private static final String UNKNOWN_BODY = "{\"status\":\"UNKNOWN\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void patchStatusWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(patchStatusRequest(null, MENGIRIM_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void patchStatusWithWrongRoleReturnsForbidden() throws Exception {
        mockMvc.perform(patchStatusRequest(NON_SUPIR_TOKEN, MENGIRIM_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void patchStatusUsesSupirUserIdFromJwtClaims() throws Exception {
        Shipment updated = new Shipment();
        updated.setId(1L);
        updated.setSupirUserId(42L);
        updated.setStatus("MENGIRIM");

        when(shipmentService.updateShipmentStatus(1L, 42L, ShipmentStatus.MENGIRIM))
                .thenReturn(updated);

        mockMvc.perform(patchStatusRequest(SUPIR_42_TOKEN, MENGIRIM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MENGIRIM"));

        verify(shipmentService).updateShipmentStatus(1L, 42L, ShipmentStatus.MENGIRIM);
    }

    @Test
    void patchStatusReturnsConflictWhenTransitionInvalid() throws Exception {
        when(shipmentService.updateShipmentStatus(1L, 42L, ShipmentStatus.TIBA))
                .thenThrow(new ShipmentInvalidTransitionException("Invalid status transition"));

        mockMvc.perform(patchStatusRequest(SUPIR_42_TOKEN, TIBA_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Invalid status transition"));
    }

    @Test
    void patchStatusReturnsBadRequestWhenStatusValueUnknown() throws Exception {
        mockMvc.perform(patchStatusRequest(SUPIR_42_TOKEN, UNKNOWN_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    private MockHttpServletRequestBuilder patchStatusRequest(String authorization, String body) {
        MockHttpServletRequestBuilder requestBuilder = patch(PATCH_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (authorization != null) {
            requestBuilder.header(AUTHORIZATION_HEADER, authorization);
        }
        return requestBuilder;
    }
}

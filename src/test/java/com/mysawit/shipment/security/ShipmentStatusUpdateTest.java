package com.mysawit.shipment.security;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.exception.ShipmentInvalidTransitionException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;

@WebMvcTest(controllers = ShipmentController.class)
@Import(JwtTokenProvider.class)
@ActiveProfiles("test")
class ShipmentStatusUpdateTest {

    private static final UUID SHIPMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUPIR_42_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final String PATCH_STATUS_PATH = "/api/shipments/" + SHIPMENT_ID + "/status";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MENGIRIM_BODY = "{\"status\":\"MENGIRIM\"}";
    private static final String TIBA_BODY = "{\"status\":\"TIBA\"}";
    private static final String UNKNOWN_BODY = "{\"status\":\"UNKNOWN\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void patchStatusWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(patchStatusRequest(null, MENGIRIM_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void patchStatusWithWrongRoleReturnsForbidden() throws Exception {
        String mandorToken = JwtFixture.mandorToken(SUPIR_42_ID.toString());

        mockMvc.perform(patchStatusRequest(BEARER_PREFIX + mandorToken, MENGIRIM_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void patchStatusUsesSupirUserIdFromJwtClaims() throws Exception {
        Shipment updated = new Shipment();
        updated.setId(SHIPMENT_ID);
        updated.setSupirUserId(SUPIR_42_ID);
        updated.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentService.updateShipmentStatus(SHIPMENT_ID, SUPIR_42_ID, ShipmentStatus.MENGIRIM))
                .thenReturn(updated);

        String supirToken = JwtFixture.supirToken(SUPIR_42_ID.toString());

        mockMvc.perform(patchStatusRequest(BEARER_PREFIX + supirToken, MENGIRIM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MENGIRIM"));

        verify(shipmentService).updateShipmentStatus(SHIPMENT_ID, SUPIR_42_ID, ShipmentStatus.MENGIRIM);
    }

    @Test
    void patchStatusReturnsConflictWhenTransitionInvalid() throws Exception {
        when(shipmentService.updateShipmentStatus(SHIPMENT_ID, SUPIR_42_ID, ShipmentStatus.TIBA))
                .thenThrow(new ShipmentInvalidTransitionException("Invalid status transition"));

        String supirToken = JwtFixture.supirToken(SUPIR_42_ID.toString());

        mockMvc.perform(patchStatusRequest(BEARER_PREFIX + supirToken, TIBA_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Invalid status transition"));
    }

    @Test
    void patchStatusReturnsBadRequestWhenStatusValueUnknown() throws Exception {
        String supirToken = JwtFixture.supirToken(SUPIR_42_ID.toString());

        mockMvc.perform(patchStatusRequest(BEARER_PREFIX + supirToken, UNKNOWN_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void patchStatusReturnsBadRequestWhenStatusMissing() throws Exception {
        String supirToken = JwtFixture.supirToken(SUPIR_42_ID.toString());

        mockMvc.perform(patchStatusRequest(BEARER_PREFIX + supirToken, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid status value"));
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

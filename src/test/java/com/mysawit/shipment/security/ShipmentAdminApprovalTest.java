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
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;

@WebMvcTest(controllers = ShipmentController.class)
@Import(JwtTokenProvider.class)
@ActiveProfiles("test")
class ShipmentAdminApprovalTest {

    private static final UUID SHIPMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SUPIR_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final String ADMIN_APPROVAL_PATH = "/api/shipments/" + SHIPMENT_ID + "/admin-approval";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String APPROVED_BODY = "{\"status\":\"ADMIN_APPROVED\"}";
    private static final String PARTIALLY_REJECTED_BODY = "{\"status\":\"PARTIALLY_REJECTED\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void patchAdminApprovalWithAdminTokenReturnsOk() throws Exception {
        Shipment approved = shipmentWithStatus(ShipmentStatus.ADMIN_APPROVED);
        when(shipmentService.approveShipmentByAdmin(SHIPMENT_ID, ShipmentStatus.ADMIN_APPROVED))
                .thenReturn(approved);

        String adminToken = JwtFixture.adminToken(ADMIN_ID.toString());

        mockMvc.perform(adminApprovalRequest(BEARER_PREFIX + adminToken, APPROVED_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_APPROVED"));

        verify(shipmentService).approveShipmentByAdmin(SHIPMENT_ID, ShipmentStatus.ADMIN_APPROVED);
    }

    @Test
    void patchAdminApprovalAllowsPartialRejectDecision() throws Exception {
        Shipment rejected = shipmentWithStatus(ShipmentStatus.PARTIALLY_REJECTED);
        when(shipmentService.approveShipmentByAdmin(SHIPMENT_ID, ShipmentStatus.PARTIALLY_REJECTED))
                .thenReturn(rejected);

        String adminToken = JwtFixture.adminToken(ADMIN_ID.toString());

        mockMvc.perform(adminApprovalRequest(BEARER_PREFIX + adminToken, PARTIALLY_REJECTED_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_REJECTED"));

        verify(shipmentService).approveShipmentByAdmin(SHIPMENT_ID, ShipmentStatus.PARTIALLY_REJECTED);
    }

    @Test
    void patchAdminApprovalWithSupirTokenReturnsForbidden() throws Exception {
        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());

        mockMvc.perform(adminApprovalRequest(BEARER_PREFIX + supirToken, APPROVED_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void patchAdminApprovalWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(adminApprovalRequest(null, APPROVED_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(shipmentService);
    }

    private Shipment shipmentWithStatus(ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setId(SHIPMENT_ID);
        shipment.setMandorUserId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        shipment.setSupirUserId(SUPIR_ID);
        shipment.setDestination("Jakarta");
        shipment.setTotalKg(100.0);
        shipment.setStatus(status);
        return shipment;
    }

    private MockHttpServletRequestBuilder adminApprovalRequest(String authorization, String body) {
        MockHttpServletRequestBuilder requestBuilder = patch(ADMIN_APPROVAL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (authorization != null) {
            requestBuilder.header(AUTHORIZATION_HEADER, authorization);
        }
        return requestBuilder;
    }
}

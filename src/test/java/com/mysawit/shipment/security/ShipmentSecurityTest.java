package com.mysawit.shipment.security;

import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.service.ShipmentService;

@WebMvcTest(controllers = ShipmentController.class)
@Import(JwtTokenProvider.class)
@ActiveProfiles("test")
class ShipmentSecurityTest {
    private static final String SHIPMENTS_PATH = "/api/shipments";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final UUID SUPIR_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentsWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(SHIPMENTS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getShipmentsWithWrongRoleReturnsForbidden() throws Exception {
        String buruhToken = JwtFixture.tokenWithRole(SUPIR_ID.toString(), "BURUH");

        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + buruhToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getShipmentsWithMandorTokenReturnsOk() throws Exception {
        String mandorToken = JwtFixture.mandorToken(SUPIR_ID.toString());
        when(shipmentService.getAllShipments()).thenReturn(List.of());

        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + mandorToken))
                .andExpect(status().isOk());
    }

    @Test
    void getShipmentsWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getShipmentsWithExpiredTokenReturnsUnauthorized() throws Exception {
        String expired = JwtFixture.expiredToken(SUPIR_ID.toString(), "SUPIR");

        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getShipmentsWithSupirTokenReturnsOk() throws Exception {
        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());
        when(shipmentService.getShipmentsBySupirUserId(SUPIR_ID)).thenReturn(List.of());

        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + supirToken))
                .andExpect(status().isOk());
    }

    @Test
    void getShipmentsWithNonUuidUserIdReturnsUnauthorized() throws Exception {
        String invalidUserIdToken = JwtFixture.supirToken("not-a-valid-uuid");

        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + invalidUserIdToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void filterSetsRoleAttributeForSupirToken() throws Exception {
        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());
        when(shipmentService.getShipmentsBySupirUserId(SUPIR_ID)).thenReturn(List.of());

        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + supirToken))
                .andExpect(status().isOk())
                .andExpect(request().attribute(ShipmentSecurityAttributes.JWT_ROLE, "SUPIR"));
    }

    @Test
    void filterSetsRoleAttributeForMandorToken() throws Exception {
        String mandorToken = JwtFixture.mandorToken(SUPIR_ID.toString());
        when(shipmentService.getAllShipments()).thenReturn(List.of());

        mockMvc.perform(get(SHIPMENTS_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + mandorToken))
                .andExpect(status().isOk())
                .andExpect(request().attribute(ShipmentSecurityAttributes.JWT_ROLE, "MANDOR"));
    }

    @Test
    void shipmentHealthWithoutTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/shipments/health"))
                .andExpect(status().isOk());
    }

    @Test
    void nonShipmentPathWithoutTokenIsNotBlockedByShipmentFilter() throws Exception {
        mockMvc.perform(get("/api/non-shipment-path"))
                .andExpect(status().isNotFound());
    }
}

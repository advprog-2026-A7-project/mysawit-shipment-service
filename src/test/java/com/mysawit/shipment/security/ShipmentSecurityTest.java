package com.mysawit.shipment.security;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentsWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getShipmentsWithWrongRoleReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .header("Authorization", "Bearer token-with-non-supir-role"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getShipmentsWithUnknownTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .header("Authorization", "Bearer unknown-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getShipmentsWithSupirTokenReturnsOk() throws Exception {
        when(shipmentService.getAllShipments()).thenReturn(List.of());

        mockMvc.perform(get("/api/shipments")
                        .header("Authorization", "Bearer token-with-supir-role"))
                .andExpect(status().isOk());
    }

    @Test
    void shipmentHealthWithoutTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/shipments/health"))
                .andExpect(status().isOk());
    }
}

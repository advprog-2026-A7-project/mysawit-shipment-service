package com.mysawit.shipment.security;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}

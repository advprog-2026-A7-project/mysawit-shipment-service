package com.mysawit.shipment.security;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentOwnershipListTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentsUsesSupirUserIdFromJwtClaims() throws Exception {
        Shipment owned = new Shipment();
        owned.setId(1L);
        owned.setSupirUserId(42L);
        owned.setHarvestId(10L);
        owned.setDestination("Pabrik A");
        owned.setTotalKg(120.0);
        owned.setStatus("MEMUAT");

        when(shipmentService.getShipmentsBySupirUserId(42L)).thenReturn(List.of(owned));

        mockMvc.perform(get("/api/shipments")
                        .header("Authorization", "Bearer token-with-supir-role-user-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supirUserId").value(42));

        verify(shipmentService).getShipmentsBySupirUserId(42L);
    }
}

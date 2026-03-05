package com.mysawit.shipment.security;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentOwnershipDetailTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentByIdUsesSupirUserIdFromJwtClaims() throws Exception {
        Shipment owned = new Shipment();
        owned.setId(11L);
        owned.setSupirUserId(42L);
        owned.setStatus("MEMUAT");

        when(shipmentService.getShipmentByIdForSupirUser(11L, 42L)).thenReturn(owned);

        mockMvc.perform(get("/api/shipments/11")
                        .header("Authorization", "Bearer token-with-supir-role-user-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supirUserId").value(42));

        verify(shipmentService).getShipmentByIdForSupirUser(11L, 42L);
    }

    @Test
    void getShipmentByIdReturnsForbiddenWhenRequesterIsNotOwner() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(11L, 99L))
                .thenThrow(new ShipmentForbiddenException("Forbidden"));

        mockMvc.perform(get("/api/shipments/11")
                        .header("Authorization", "Bearer token-with-supir-role-user-99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }
}

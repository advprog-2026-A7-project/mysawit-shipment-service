package com.mysawit.shipment.security;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mysawit.shipment.controller.ShipmentController;
import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;

@WebMvcTest(controllers = ShipmentController.class)
@Import(JwtTokenProvider.class)
@ActiveProfiles("test")
class ShipmentOwnershipDetailTest {

    private static final UUID SHIPMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final UUID NON_OWNER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentByIdUsesSupirUserIdFromJwtClaims() throws Exception {
        Shipment owned = new Shipment();
        owned.setId(SHIPMENT_ID);
        owned.setSupirUserId(OWNER_ID);
        owned.setStatus(ShipmentStatus.MEMUAT);

        when(shipmentService.getShipmentByIdForSupirUser(SHIPMENT_ID, OWNER_ID)).thenReturn(owned);

        String ownerToken = JwtFixture.supirToken(OWNER_ID.toString());

        mockMvc.perform(get("/api/shipments/" + SHIPMENT_ID)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supirUserId").value(OWNER_ID.toString()));

        verify(shipmentService).getShipmentByIdForSupirUser(SHIPMENT_ID, OWNER_ID);
    }

    @Test
    void getShipmentByIdReturnsForbiddenWhenRequesterIsNotOwner() throws Exception {
        when(shipmentService.getShipmentByIdForSupirUser(SHIPMENT_ID, NON_OWNER_ID))
                .thenThrow(new ShipmentForbiddenException("Forbidden"));

        String nonOwnerToken = JwtFixture.supirToken(NON_OWNER_ID.toString());

        mockMvc.perform(get("/api/shipments/" + SHIPMENT_ID)
                        .header("Authorization", "Bearer " + nonOwnerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }
}

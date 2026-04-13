package com.mysawit.shipment.security;

import java.util.List;
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
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;

@WebMvcTest(controllers = ShipmentController.class)
@Import(JwtTokenProvider.class)
@ActiveProfiles("test")
class ShipmentOwnershipListTest {

    private static final UUID SHIPMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUPIR_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final UUID HARVEST_ID = UUID.fromString("10101010-1010-1010-1010-101010101010");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void getShipmentsUsesSupirUserIdFromJwtClaims() throws Exception {
        Shipment owned = new Shipment();
        owned.setId(SHIPMENT_ID);
        owned.setSupirUserId(SUPIR_ID);
        owned.setHarvestId(HARVEST_ID);
        owned.setDestination("Pabrik A");
        owned.setTotalKg(120.0);
        owned.setStatus(ShipmentStatus.MEMUAT);

        when(shipmentService.getShipmentsBySupirUserId(SUPIR_ID)).thenReturn(List.of(owned));

        String supirToken = JwtFixture.supirToken(SUPIR_ID.toString());

        mockMvc.perform(get("/api/shipments")
                        .header("Authorization", "Bearer " + supirToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supirUserId").value(SUPIR_ID.toString()));

        verify(shipmentService).getShipmentsBySupirUserId(SUPIR_ID);
    }
}

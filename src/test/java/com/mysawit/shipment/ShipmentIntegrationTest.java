package com.mysawit.shipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShipmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @BeforeEach
    void cleanDatabase() {
        shipmentRepository.deleteAll();
    }

    @Test
    void shipmentCrudFlowWorksEndToEnd() throws Exception {
        String created = mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "harvestId": 101,
                                  "destination": "Mill A",
                                  "weight": 900.5,
                                  "status": "IN_TRANSIT",
                                  "shipperName": "Budi",
                                  "vehicleNumber": "B 1234 SAW",
                                  "shipmentDate": "2026-05-22T11:00:00",
                                  "deliveryDate": "2026-05-22T15:00:00",
                                  "notes": "Keep sealed"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.harvestId").value(101))
                .andExpect(jsonPath("$.destination").value("Mill A"))
                .andExpect(jsonPath("$.weight").value(900.5))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.shipperName").value("Budi"))
                .andExpect(jsonPath("$.vehicleNumber").value("B 1234 SAW"))
                .andExpect(jsonPath("$.shipmentDate").value("2026-05-22T11:00:00"))
                .andExpect(jsonPath("$.deliveryDate").value("2026-05-22T15:00:00"))
                .andExpect(jsonPath("$.notes").value("Keep sealed"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "harvestId": 102,
                                  "destination": "Mill B",
                                  "weight": 300.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/shipments").param("harvestId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/shipments").param("status", "IN_TRANSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/shipments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/shipments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "harvestId": 201,
                                  "destination": "Mill C",
                                  "weight": 1000.75,
                                  "status": "DELIVERED",
                                  "shipperName": "Sari",
                                  "vehicleNumber": "B 9876 SAW",
                                  "shipmentDate": "2026-06-01T11:00:00",
                                  "deliveryDate": "2026-06-01T16:00:00",
                                  "notes": "Arrived"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.harvestId").value(201))
                .andExpect(jsonPath("$.destination").value("Mill C"))
                .andExpect(jsonPath("$.weight").value(1000.75))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.shipperName").value("Sari"))
                .andExpect(jsonPath("$.vehicleNumber").value("B 9876 SAW"))
                .andExpect(jsonPath("$.shipmentDate").value("2026-06-01T11:00:00"))
                .andExpect(jsonPath("$.deliveryDate").value("2026-06-01T16:00:00"))
                .andExpect(jsonPath("$.notes").value("Arrived"));

        mockMvc.perform(delete("/api/shipments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Shipment deleted successfully"));

        mockMvc.perform(get("/api/shipments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Shipment not found with id: " + id));
    }

    @Test
    void validationAndMissingResourcePathsReturnErrors() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "harvestId": null,
                                  "destination": "",
                                  "weight": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/shipments/{id}", 404)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "harvestId": 1,
                                  "destination": "Nowhere",
                                  "weight": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Shipment not found with id: 404"));

        mockMvc.perform(delete("/api/shipments/{id}", 404))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Shipment not found with id: 404"));
    }

    @Test
    void healthEndpointReportsServiceName() throws Exception {
        mockMvc.perform(get("/api/shipments/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("mysawit-shipment-service"));
    }
}

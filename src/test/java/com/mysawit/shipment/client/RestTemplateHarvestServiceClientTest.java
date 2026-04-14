package com.mysawit.shipment.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.mysawit.shipment.exception.HarvestServiceUnavailableException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTemplateHarvestServiceClientTest {

    private static final UUID FOREMAN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID HARVEST_A = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID HARVEST_B = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RestTemplateHarvestServiceClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RestTemplateHarvestServiceClient(restTemplate, "http://localhost:8083/");
    }

    @Test
    void getHarvestsByIdsReturnsOnlyRequestedHarvestsAndNormalizesStatuses() {
        server.expect(requestTo("http://localhost:8083/harvests"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Foreman-Id", FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        [
                          {"id":"cccccccc-cccc-cccc-cccc-cccccccccccc","status":"APPROVED"},
                          {"id":"dddddddd-dddd-dddd-dddd-dddddddddddd","status":"PENDING"},
                          {"id":"eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee","status":"REJECTED"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        Map<UUID, HarvestServiceClient.HarvestDetails> harvests = client.getHarvestsByIds(
                FOREMAN_ID,
                List.of(HARVEST_A, HARVEST_B)
        );

        assertEquals(2, harvests.size());
        assertEquals("Approved", harvests.get(HARVEST_A).status());
        assertEquals("Pending", harvests.get(HARVEST_B).status());
        server.verify();
    }

    @Test
    void getHarvestsByIdsReturnsEmptyMapWhenNoIdsRequested() {
        Map<UUID, HarvestServiceClient.HarvestDetails> harvests = client.getHarvestsByIds(FOREMAN_ID, List.of());

        assertTrue(harvests.isEmpty());
    }

    @Test
    void getHarvestsByIdsThrowsUnavailableWhenRemoteCallFails() {
        RestTemplateHarvestServiceClient failingClient =
                new RestTemplateHarvestServiceClient(new RestTemplate(), "http://localhost:1");

        assertThrows(
                HarvestServiceUnavailableException.class,
                () -> failingClient.getHarvestsByIds(FOREMAN_ID, List.of(HARVEST_A))
        );
    }
}

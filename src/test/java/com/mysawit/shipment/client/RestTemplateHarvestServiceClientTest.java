package com.mysawit.shipment.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.mysawit.shipment.exception.HarvestServiceUnavailableException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTemplateHarvestServiceClientTest {

    private static final String HARVEST_SERVICE_BASE_URL = "http://localhost:8083";
    private static final String HARVESTS_URL = HARVEST_SERVICE_BASE_URL + "/harvests";
    private static final String FOREMAN_HEADER = "X-Foreman-Id";
    private static final UUID FOREMAN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID HARVEST_A = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID HARVEST_B = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID HARVEST_C = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RestTemplateHarvestServiceClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RestTemplateHarvestServiceClient(restTemplate, HARVEST_SERVICE_BASE_URL + "/");
    }

    @Test
    void getHarvestsByIdsReturnsOnlyRequestedHarvestsAndNormalizesStatuses() {
        server.expect(requestTo(HARVESTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
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
    void getHarvestsByIdsReturnsEmptyMapWhenRequestedIdsAreNull() {
        Map<UUID, HarvestServiceClient.HarvestDetails> harvests = client.getHarvestsByIds(FOREMAN_ID, null);

        assertTrue(harvests.isEmpty());
    }

    @Test
    void builderConstructorTrimsTrailingSlashFromBaseUrl() {
        RestTemplateHarvestServiceClient builderClient = new RestTemplateHarvestServiceClient(
                new RestTemplateBuilder(),
                HARVEST_SERVICE_BASE_URL + "/",
                java.time.Duration.ofSeconds(2),
                java.time.Duration.ofSeconds(2)
        );

        assertEquals(
                HARVEST_SERVICE_BASE_URL,
                ReflectionTestUtils.getField(builderClient, "harvestServiceBaseUrl")
        );
    }

    @Test
    void getHarvestsByIdsReturnsEmptyMapWhenRemoteResponseBodyIsEmpty() {
        server.expect(requestTo(HARVESTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Map<UUID, HarvestServiceClient.HarvestDetails> harvests = client.getHarvestsByIds(
                FOREMAN_ID,
                List.of(HARVEST_A)
        );

        assertTrue(harvests.isEmpty());
        server.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getHarvestsByIdsReturnsEmptyMapWhenRemoteResponseBodyIsNull() {
        RestTemplate mockedRestTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        org.mockito.Mockito.when(mockedRestTemplate.exchange(
                org.mockito.Mockito.eq(HARVESTS_URL),
                org.mockito.Mockito.eq(HttpMethod.GET),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.<org.springframework.core.ParameterizedTypeReference<List<?>>>any()
        )).thenReturn((ResponseEntity) ResponseEntity.ok().build());
        RestTemplateHarvestServiceClient mockedClient =
                new RestTemplateHarvestServiceClient(mockedRestTemplate, HARVEST_SERVICE_BASE_URL);

        Map<UUID, HarvestServiceClient.HarvestDetails> harvests =
                mockedClient.getHarvestsByIds(FOREMAN_ID, List.of(HARVEST_A));

        assertTrue(harvests.isEmpty());
    }

    @Test
    void getHarvestsByIdsKeepsUnknownStatusAndNullStatusWhileIgnoringUnrequestedPayloads() {
        server.expect(requestTo(HARVESTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        [
                          {"id":"cccccccc-cccc-cccc-cccc-cccccccccccc","status":"CUSTOM"},
                          {"id":"dddddddd-dddd-dddd-dddd-dddddddddddd","status":null},
                          {"id":"ffffffff-ffff-ffff-ffff-ffffffffffff","status":"APPROVED"},
                          {"id":null,"status":"APPROVED"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        Map<UUID, HarvestServiceClient.HarvestDetails> harvests = client.getHarvestsByIds(
                FOREMAN_ID,
                List.of(HARVEST_A, HARVEST_B, HARVEST_C)
        );

        assertEquals("CUSTOM", harvests.get(HARVEST_A).status());
        assertEquals(null, harvests.get(HARVEST_B).status());
        assertTrue(!harvests.containsKey(HARVEST_C));
        server.verify();
    }

    @Test
    void getHarvestsByIdsNormalizesRejectedStatusForRequestedHarvests() {
        server.expect(requestTo(HARVESTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        [
                          {"id":"eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee","status":"REJECTED"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        Map<UUID, HarvestServiceClient.HarvestDetails> harvests = client.getHarvestsByIds(
                FOREMAN_ID,
                List.of(HARVEST_C)
        );

        assertEquals("Rejected", harvests.get(HARVEST_C).status());
        server.verify();
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

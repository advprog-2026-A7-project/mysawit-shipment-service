package com.mysawit.shipment.client;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.mysawit.shipment.exception.HarvestServiceUnavailableException;
import com.mysawit.shipment.exception.HarvestValidationException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTemplateHarvestServiceClientTest {

    private static final String HARVEST_SERVICE_BASE_URL = "http://localhost:8083";
    private static final String HARVEST_COULD_NOT_BE_VALIDATED_PREFIX = "Harvest could not be validated: ";
    private static final String HARVEST_NOT_FOUND_PREFIX = "Harvest not found: ";
    private static final String FOREMAN_HEADER = "X-Foreman-Id";
    private static final UUID FOREMAN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID HARVEST_A = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID HARVEST_B = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID HARVEST_C = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final String HARVEST_A_URL = HARVEST_SERVICE_BASE_URL + "/harvests/" + HARVEST_A;
    private static final String HARVEST_B_URL = HARVEST_SERVICE_BASE_URL + "/harvests/" + HARVEST_B;
    private static final String HARVEST_C_URL = HARVEST_SERVICE_BASE_URL + "/harvests/" + HARVEST_C;

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
    void getHarvestByIdReturnsRequestedHarvestAndNormalizesStatus() {
        server.expect(requestTo(HARVEST_A_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        {"id":"cccccccc-cccc-cccc-cccc-cccccccccccc","status":"APPROVED"}
                        """, MediaType.APPLICATION_JSON));

        HarvestServiceClient.HarvestDetails harvest = client.getHarvestById(FOREMAN_ID, HARVEST_A);

        assertEquals(HARVEST_A, harvest.id());
        assertEquals("Approved", harvest.status());
        server.verify();
    }

    @Test
    void getHarvestByIdReturnsNullStatusWhenRemoteStatusIsNull() {
        server.expect(requestTo(HARVEST_B_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        {"id":"dddddddd-dddd-dddd-dddd-dddddddddddd","status":null}
                        """, MediaType.APPLICATION_JSON));

        HarvestServiceClient.HarvestDetails harvest = client.getHarvestById(FOREMAN_ID, HARVEST_B);

        assertEquals(HARVEST_B, harvest.id());
        assertNull(harvest.status());
        server.verify();
    }

    @Test
    void getHarvestByIdNormalizesPendingStatus() {
        server.expect(requestTo(HARVEST_B_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        {"id":"dddddddd-dddd-dddd-dddd-dddddddddddd","status":"PENDING"}
                        """, MediaType.APPLICATION_JSON));

        HarvestServiceClient.HarvestDetails harvest = client.getHarvestById(FOREMAN_ID, HARVEST_B);

        assertEquals("Pending", harvest.status());
        server.verify();
    }

    @Test
    void getHarvestByIdNormalizesRejectedStatus() {
        server.expect(requestTo(HARVEST_C_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        {"id":"eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee","status":"REJECTED"}
                        """, MediaType.APPLICATION_JSON));

        HarvestServiceClient.HarvestDetails harvest = client.getHarvestById(FOREMAN_ID, HARVEST_C);

        assertEquals("Rejected", harvest.status());
        server.verify();
    }

    @Test
    void getHarvestByIdKeepsUnknownStatus() {
        server.expect(requestTo(HARVEST_C_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withSuccess("""
                        {"id":"eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee","status":"CUSTOM"}
                        """, MediaType.APPLICATION_JSON));

        HarvestServiceClient.HarvestDetails harvest = client.getHarvestById(FOREMAN_ID, HARVEST_C);

        assertEquals("CUSTOM", harvest.status());
        server.verify();
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
    void getHarvestByIdThrowsNotFoundWhenRemoteReturns404() {
        server.expect(requestTo(HARVEST_A_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> client.getHarvestById(FOREMAN_ID, HARVEST_A)
        );

        assertEquals(HARVEST_NOT_FOUND_PREFIX + HARVEST_A, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        server.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getHarvestByIdThrowsNotFoundWhenRemoteResponseBodyIsNull() {
        RestTemplate mockedRestTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        org.mockito.Mockito.when(mockedRestTemplate.exchange(
                org.mockito.Mockito.eq(HARVEST_A_URL),
                org.mockito.Mockito.eq(HttpMethod.GET),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(Class.class)
        )).thenReturn((ResponseEntity) ResponseEntity.ok().build());
        RestTemplateHarvestServiceClient mockedClient =
                new RestTemplateHarvestServiceClient(mockedRestTemplate, HARVEST_SERVICE_BASE_URL);

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> mockedClient.getHarvestById(FOREMAN_ID, HARVEST_A)
        );

        assertEquals(HARVEST_NOT_FOUND_PREFIX + HARVEST_A, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getHarvestByIdThrowsBadRequestWhenRemoteReturnsClientError() {
        server.expect(requestTo(HARVEST_A_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        HarvestValidationException exception = assertThrows(
                HarvestValidationException.class,
                () -> client.getHarvestById(FOREMAN_ID, HARVEST_A)
        );

        assertEquals(HARVEST_COULD_NOT_BE_VALIDATED_PREFIX + HARVEST_A, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        server.verify();
    }

    @Test
    void getHarvestByIdThrowsUnavailableWhenRemoteReturnsServerError() {
        server.expect(requestTo(HARVEST_A_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(FOREMAN_HEADER, FOREMAN_ID.toString()))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(
                HarvestServiceUnavailableException.class,
                () -> client.getHarvestById(FOREMAN_ID, HARVEST_A)
        );

        server.verify();
    }

    @Test
    void getHarvestByIdThrowsUnavailableWhenRemoteCallFails() {
        RestTemplate mockedRestTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        org.mockito.Mockito.when(mockedRestTemplate.exchange(
                org.mockito.Mockito.eq(HARVEST_A_URL),
                org.mockito.Mockito.eq(HttpMethod.GET),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(Class.class)
        )).thenThrow(new RestClientException("down"));
        RestTemplateHarvestServiceClient failingClient =
                new RestTemplateHarvestServiceClient(mockedRestTemplate, HARVEST_SERVICE_BASE_URL);

        assertThrows(
                HarvestServiceUnavailableException.class,
                () -> failingClient.getHarvestById(FOREMAN_ID, HARVEST_A)
        );
    }
}

package com.mysawit.shipment.client;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.mysawit.shipment.exception.HarvestServiceUnavailableException;
import com.mysawit.shipment.exception.HarvestValidationException;

@Service
public class RestTemplateHarvestServiceClient implements HarvestServiceClient {

    private static final String HARVESTS_PATH = "/harvests";
    private static final String HARVEST_SERVICE_UNAVAILABLE = "Harvest service is unavailable";
    private static final String HARVEST_NOT_FOUND_PREFIX = "Harvest not found: ";
    private static final String HARVEST_COULD_NOT_BE_VALIDATED_PREFIX = "Harvest could not be validated: ";
    private static final String FOREMAN_HEADER = "X-Foreman-Id";
    private static final Map<String, String> NORMALIZED_STATUSES = Map.of(
            "PENDING", "Pending",
            "APPROVED", "Approved",
            "REJECTED", "Rejected"
    );

    private final RestTemplate restTemplate;
    private final String harvestServiceBaseUrl;

    public RestTemplateHarvestServiceClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${harvest.service.base-url:http://localhost:8083}") String harvestServiceBaseUrl,
            @Value("${harvest.service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${harvest.service.read-timeout:2s}") Duration readTimeout
    ) {
        this(
                restTemplateBuilder
                        .connectTimeout(connectTimeout)
                        .readTimeout(readTimeout)
                        .build(),
                harvestServiceBaseUrl
        );
    }

    RestTemplateHarvestServiceClient(RestTemplate restTemplate, String harvestServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.harvestServiceBaseUrl = trimTrailingSlash(harvestServiceBaseUrl);
    }

    @Override
    public HarvestDetails getHarvestById(UUID foremanId, UUID harvestId) {
        try {
            HarvestPayload harvest = restTemplate.exchange(
                    harvestUrl(harvestId),
                    HttpMethod.GET,
                    requestEntity(foremanId),
                    HarvestPayload.class
            ).getBody();
            if (harvest == null) {
                throw HarvestValidationException.notFound(HARVEST_NOT_FOUND_PREFIX + harvestId);
            }
            return new HarvestDetails(harvest.id(), normalizeStatus(harvest.status()));
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw toHarvestValidationException(harvestId, ex);
            }
            throw new HarvestServiceUnavailableException(HARVEST_SERVICE_UNAVAILABLE, ex);
        } catch (RestClientException ex) {
            throw new HarvestServiceUnavailableException(HARVEST_SERVICE_UNAVAILABLE, ex);
        }
    }

    private HttpEntity<Void> requestEntity(UUID foremanId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(FOREMAN_HEADER, foremanId.toString());
        return new HttpEntity<>(headers);
    }

    private String harvestUrl(UUID harvestId) {
        return harvestServiceBaseUrl + HARVESTS_PATH + "/" + harvestId;
    }

    private HarvestValidationException toHarvestValidationException(UUID harvestId, HttpStatusCodeException ex) {
        if (ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return HarvestValidationException.notFound(HARVEST_NOT_FOUND_PREFIX + harvestId);
        }
        return HarvestValidationException.badRequest(HARVEST_COULD_NOT_BE_VALIDATED_PREFIX + harvestId);
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null) {
            return null;
        }

        return NORMALIZED_STATUSES.getOrDefault(rawStatus, rawStatus);
    }

    private String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private record HarvestPayload(UUID id, String status) {
    }
}

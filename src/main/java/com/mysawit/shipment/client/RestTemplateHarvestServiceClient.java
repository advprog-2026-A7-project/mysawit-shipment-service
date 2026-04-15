package com.mysawit.shipment.client;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final RestTemplate restTemplate;
    private final String harvestServiceBaseUrl;

    @Autowired
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
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Foreman-Id", foremanId.toString());

            HarvestPayload harvest = restTemplate.exchange(
                    harvestServiceBaseUrl + HARVESTS_PATH + "/" + harvestId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    HarvestPayload.class
            ).getBody();
            if (harvest == null) {
                throw new HarvestValidationException("Harvest not found: " + harvestId, HttpStatus.NOT_FOUND);
            }
            return new HarvestDetails(harvest.id(), normalizeStatus(harvest.status()));
        } catch (HttpStatusCodeException ex) {
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                throw new HarvestValidationException("Harvest not found: " + harvestId, HttpStatus.NOT_FOUND);
            }
            if (ex.getStatusCode().is4xxClientError()) {
                throw new HarvestValidationException("Harvest could not be validated: " + harvestId, HttpStatus.BAD_REQUEST);
            }
            throw new HarvestServiceUnavailableException("Harvest service is unavailable", ex);
        } catch (RestClientException ex) {
            throw new HarvestServiceUnavailableException("Harvest service is unavailable", ex);
        }
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null) {
            return null;
        }

        return switch (rawStatus) {
            case "PENDING" -> "Pending";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            default -> rawStatus;
        };
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

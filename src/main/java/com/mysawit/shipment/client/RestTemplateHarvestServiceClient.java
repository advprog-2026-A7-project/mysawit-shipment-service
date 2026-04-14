package com.mysawit.shipment.client;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.mysawit.shipment.exception.HarvestServiceUnavailableException;

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
    public Map<UUID, HarvestDetails> getHarvestsByIds(UUID foremanId, List<UUID> harvestIds) {
        if (harvestIds == null || harvestIds.isEmpty()) {
            return Map.of();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Foreman-Id", foremanId.toString());

            ResponseEntity<List<HarvestPayload>> response = restTemplate.exchange(
                    harvestServiceBaseUrl + HARVESTS_PATH,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<HarvestPayload> harvests = response.getBody();
            if (harvests == null || harvests.isEmpty()) {
                return Map.of();
            }

            return toRequestedHarvests(harvestIds, harvests);
        } catch (RestClientException ex) {
            throw new HarvestServiceUnavailableException("Harvest service is unavailable", ex);
        }
    }

    private Map<UUID, HarvestDetails> toRequestedHarvests(List<UUID> harvestIds, List<HarvestPayload> harvests) {
        Set<UUID> requestedIds = Set.copyOf(harvestIds);
        Map<UUID, HarvestDetails> result = new LinkedHashMap<>();
        for (HarvestPayload harvest : harvests) {
            if (harvest.id() != null && requestedIds.contains(harvest.id())) {
                result.put(harvest.id(), new HarvestDetails(harvest.id(), normalizeStatus(harvest.status())));
            }
        }
        return result;
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

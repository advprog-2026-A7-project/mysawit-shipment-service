package com.mysawit.shipment.controller;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.security.ShipmentSecurityAttributes;
import com.mysawit.shipment.service.ShipmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private static final String STATUS_FIELD = "status";
    
    private final ShipmentService shipmentService;
    
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }
    
    @GetMapping
    public ResponseEntity<List<Shipment>> getAllShipments(HttpServletRequest request) {
        UUID requesterUserId = extractRequesterUserId(request);
        if (requesterUserId != null) {
            return ResponseEntity.ok(shipmentService.getShipmentsBySupirUserId(requesterUserId));
        }
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(@PathVariable UUID id, HttpServletRequest request) {
        UUID requesterUserId = extractRequesterUserId(request);
        if (requesterUserId != null) {
            return ResponseEntity.ok(shipmentService.getShipmentByIdForSupirUser(id, requesterUserId));
        }
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Shipment> updateShipmentStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request
    ) {
        UUID requesterUserId = extractRequesterUserId(request);
        ShipmentStatus targetStatus = parseStatus(requestBody);
        return ResponseEntity.ok(shipmentService.updateShipmentStatus(id, requesterUserId, targetStatus));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "mysawit-shipment-service");
        return ResponseEntity.ok(health);
    }

    private UUID extractRequesterUserId(HttpServletRequest request) {
        Object userIdAttr = request == null ? null : request.getAttribute(ShipmentSecurityAttributes.JWT_USER_ID);
        if (userIdAttr instanceof UUID userId) {
            return userId;
        }
        return null;
    }

    private ShipmentStatus parseStatus(Map<String, String> requestBody) {
        String statusValue = requestBody.get(STATUS_FIELD);
        if (statusValue == null || statusValue.isBlank()) {
            throw new IllegalArgumentException("Invalid status value");
        }
        return ShipmentStatus.valueOf(statusValue);
    }
}

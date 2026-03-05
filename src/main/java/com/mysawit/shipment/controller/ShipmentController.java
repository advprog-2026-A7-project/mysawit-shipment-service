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

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    
    private final ShipmentService shipmentService;
    
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }
    
    @GetMapping
    public ResponseEntity<List<Shipment>> getAllShipments(HttpServletRequest request) {
        Long requesterUserId = extractRequesterUserId(request);
        if (requesterUserId != null) {
            Long userId = requesterUserId;
            return ResponseEntity.ok(shipmentService.getShipmentsBySupirUserId(userId));
        }
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(@PathVariable Long id, HttpServletRequest request) {
        Long requesterUserId = extractRequesterUserId(request);
        if (requesterUserId != null) {
            Long userId = requesterUserId;
            return ResponseEntity.ok(shipmentService.getShipmentByIdForSupirUser(id, userId));
        }
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Shipment> updateShipmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request
    ) {
        Long requesterUserId = (Long) request.getAttribute(ShipmentSecurityAttributes.JWT_USER_ID);
        ShipmentStatus targetStatus = ShipmentStatus.valueOf(requestBody.get("status"));
        return ResponseEntity.ok(shipmentService.updateShipmentStatus(id, requesterUserId, targetStatus));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "mysawit-shipment-service");
        return ResponseEntity.ok(health);
    }

    private Long extractRequesterUserId(HttpServletRequest request) {
        Object userIdAttr = request == null ? null : request.getAttribute(ShipmentSecurityAttributes.JWT_USER_ID);
        if (userIdAttr instanceof Long userId) {
            return userId;
        }
        return null;
    }
}

package com.mysawit.shipment.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.dto.AdminApprovalRequest;
import com.mysawit.shipment.dto.CreateShipmentRequest;
import com.mysawit.shipment.dto.ShipmentResponse;
import com.mysawit.shipment.dto.UpdateStatusRequest;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.security.ShipmentSecurityAttributes;
import com.mysawit.shipment.service.ShipmentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private static final String ERR_FORBIDDEN = "Forbidden";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_SUPIR = "SUPIR";

    private final ShipmentService shipmentService;
    
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }
    
    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments(HttpServletRequest request) {
        String role = extractRequesterRole(request);
        UUID requesterUserId = extractRequesterUserId(request);
        if (ROLE_SUPIR.equals(role) && requesterUserId != null) {
            return ResponseEntity.ok(shipmentService.getShipmentsBySupirUserId(requesterUserId)
                    .stream().map(ShipmentResponse::fromEntity).toList());
        }
        return ResponseEntity.ok(shipmentService.getAllShipments()
                .stream().map(ShipmentResponse::fromEntity).toList());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable UUID id, HttpServletRequest request) {
        String role = extractRequesterRole(request);
        UUID requesterUserId = extractRequesterUserId(request);
        if (ROLE_SUPIR.equals(role) && requesterUserId != null) {
            return ResponseEntity.ok(ShipmentResponse.fromEntity(
                    shipmentService.getShipmentByIdForSupirUser(id, requesterUserId)));
        }
        return ResponseEntity.ok(ShipmentResponse.fromEntity(shipmentService.getShipmentById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest requestBody,
            HttpServletRequest request
    ) {
        requireRole(request, ROLE_SUPIR);
        UUID requesterUserId = extractRequesterUserId(request);
        ShipmentStatus targetStatus = parseStatus(requestBody);
        return ResponseEntity.ok(ShipmentResponse.fromEntity(
                shipmentService.updateShipmentStatus(id, requesterUserId, targetStatus)));
    }
    
    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            HttpServletRequest httpRequest
    ) {
        requireRole(httpRequest, ROLE_MANDOR);
        UUID mandorUserId = extractRequesterUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ShipmentResponse.fromEntity(
                        shipmentService.createShipment(mandorUserId, request)));
    }

    @PatchMapping("/{id}/admin-approval")
    public ResponseEntity<ShipmentResponse> approveShipmentByAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody AdminApprovalRequest requestBody,
            HttpServletRequest request
    ) {
        requireRole(request, ROLE_ADMIN);
        ShipmentStatus decision = parseStatus(requestBody.status());
        return ResponseEntity.ok(ShipmentResponse.fromEntity(
                shipmentService.approveShipmentByAdmin(id, decision)));
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

    private String extractRequesterRole(HttpServletRequest request) {
        Object roleAttr = request == null ? null : request.getAttribute(ShipmentSecurityAttributes.JWT_ROLE);
        if (roleAttr instanceof String role) {
            return role;
        }
        return null;
    }

    private ShipmentStatus parseStatus(UpdateStatusRequest requestBody) {
        return parseStatus(requestBody.status());
    }

    private ShipmentStatus parseStatus(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            throw new IllegalArgumentException("Invalid status value");
        }
        return ShipmentStatus.valueOf(statusValue);
    }

    private void requireRole(HttpServletRequest request, String expectedRole) {
        if (!expectedRole.equals(extractRequesterRole(request))) {
            throw new ShipmentForbiddenException(ERR_FORBIDDEN);
        }
    }
}

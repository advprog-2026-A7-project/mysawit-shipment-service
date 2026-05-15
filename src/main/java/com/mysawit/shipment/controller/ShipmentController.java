package com.mysawit.shipment.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.dto.AdminApprovalRequest;
import com.mysawit.shipment.dto.CreateShipmentRequest;
import com.mysawit.shipment.dto.MandorApprovalRequest;
import com.mysawit.shipment.dto.ShipmentResponse;
import com.mysawit.shipment.dto.SupirAssignmentResponse;
import com.mysawit.shipment.dto.UpdateStatusRequest;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.security.ShipmentSecurityAttributes;
import com.mysawit.shipment.security.ShipmentRoles;
import com.mysawit.shipment.service.ShipmentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private static final String ERR_FORBIDDEN = "Forbidden";
    private static final String ERR_INVALID_STATUS_VALUE = "Invalid status value";

    private final ShipmentService shipmentService;
    
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }
    
    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments(
            @RequestParam(required = false) String mandorName,
            @RequestParam(required = false) String supirName,
            @RequestParam(required = false) UUID supirUserId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpServletRequest request
    ) {
        String role = extractRequesterRole(request);
        UUID requesterUserId = extractRequesterUserId(request);
        if (ShipmentRoles.SUPIR.equals(role) && requesterUserId != null) {
            return ResponseEntity.ok(toResponses(shipmentService.getShipmentsBySupirUserId(
                    requesterUserId,
                    date,
                    parseOptionalStatus(status)
            )));
        }
        if (ShipmentRoles.MANDOR.equals(role) && requesterUserId != null) {
            return ResponseEntity.ok(toResponses(shipmentService.getShipmentsByMandorUserId(
                    requesterUserId,
                    supirUserId,
                    supirName,
                    date,
                    parseOptionalStatus(status)
            )));
        }
        if (ShipmentRoles.ADMIN.equals(role)) {
            return ResponseEntity.ok(toResponses(shipmentService.getShipmentsForAdmin(
                    mandorName,
                    date,
                    parseOptionalStatus(status)
            )));
        }
        return ResponseEntity.ok(toResponses(shipmentService.getAllShipments()));
    }

    public ResponseEntity<List<ShipmentResponse>> getAllShipments(HttpServletRequest request) {
        return getAllShipments(null, null, null, null, null, request);
    }

    @GetMapping("/available-supirs")
    public ResponseEntity<List<SupirAssignmentResponse>> getAvailableSupirs(
            @RequestParam(required = false) String name,
            HttpServletRequest request
    ) {
        requireRole(request, ShipmentRoles.MANDOR);
        UUID mandorUserId = extractRequesterUserId(request);
        return ResponseEntity.ok(shipmentService.getSupirsForMandor(mandorUserId, name).stream()
                .map(SupirAssignmentResponse::fromEntity)
                .toList());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable UUID id, HttpServletRequest request) {
        String role = extractRequesterRole(request);
        UUID requesterUserId = extractRequesterUserId(request);
        if (ShipmentRoles.SUPIR.equals(role) && requesterUserId != null) {
            return ResponseEntity.ok(ShipmentResponse.fromEntity(
                    shipmentService.getShipmentByIdForSupirUser(id, requesterUserId)));
        }
        return ResponseEntity.ok(ShipmentResponse.fromEntity(shipmentService.getShipmentById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest requestBody,
            HttpServletRequest request
    ) {
        requireRole(request, ShipmentRoles.SUPIR);
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
        requireRole(httpRequest, ShipmentRoles.MANDOR);
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
        requireRole(request, ShipmentRoles.ADMIN);
        ShipmentStatus decision = parseStatus(requestBody.status());
        return ResponseEntity.ok(ShipmentResponse.fromEntity(
                shipmentService.approveShipmentByAdmin(
                        id,
                        decision,
                        requestBody.rejectionReason(),
                        requestBody.kgAccepted()
                )));
    }

    @PatchMapping("/{id}/mandor-approval")
    public ResponseEntity<ShipmentResponse> approveShipmentByMandor(
            @PathVariable UUID id,
            @Valid @RequestBody MandorApprovalRequest requestBody,
            HttpServletRequest request
    ) {
        requireRole(request, ShipmentRoles.MANDOR);
        UUID requesterUserId = extractRequesterUserId(request);
        ShipmentStatus decision = parseStatus(requestBody.status());
        return ResponseEntity.ok(ShipmentResponse.fromEntity(
                shipmentService.approveShipmentByMandor(
                        id,
                        requesterUserId,
                        decision,
                        requestBody.rejectionReason()
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "mysawit-shipment-service"
        ));
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
            throw new IllegalArgumentException(ERR_INVALID_STATUS_VALUE);
        }
        try {
            return ShipmentStatus.valueOf(statusValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ERR_INVALID_STATUS_VALUE, ex);
        }
    }

    private ShipmentStatus parseOptionalStatus(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return null;
        }
        return parseStatus(statusValue);
    }

    private void requireRole(HttpServletRequest request, String expectedRole) {
        if (!expectedRole.equals(extractRequesterRole(request))) {
            throw new ShipmentForbiddenException(ERR_FORBIDDEN);
        }
    }

    private List<ShipmentResponse> toResponses(List<Shipment> shipments) {
        return shipments.stream()
                .map(ShipmentResponse::fromEntity)
                .toList();
    }
}

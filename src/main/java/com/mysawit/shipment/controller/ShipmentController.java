package com.mysawit.shipment.controller;

import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.exception.ShipmentForbiddenException;
import com.mysawit.shipment.exception.ShipmentNotFoundException;
import com.mysawit.shipment.security.ShipmentSecurityAttributes;
import com.mysawit.shipment.service.ShipmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
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
        Object userIdAttr = request == null ? null : request.getAttribute(ShipmentSecurityAttributes.JWT_USER_ID);
        if (userIdAttr instanceof Long userId) {
            return ResponseEntity.ok(shipmentService.getShipmentsBySupirUserId(userId));
        }
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getShipmentById(@PathVariable Long id) {
        try {
            Shipment shipment = shipmentService.getShipmentById(id);
            return ResponseEntity.ok(shipment);
        } catch (ShipmentNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (ShipmentForbiddenException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "FORBIDDEN");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "mysawit-shipment-service");
        return ResponseEntity.ok(health);
    }
}

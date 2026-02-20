package com.mysawit.shipment.controller;

import com.mysawit.shipment.dto.ShipmentRequest;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<Shipment>> getAllShipments(
            @RequestParam(required = false) Long harvestId,
            @RequestParam(required = false) String status) {
        List<Shipment> shipments;
        if (harvestId != null) {
            shipments = shipmentService.getShipmentsByHarvestId(harvestId);
        } else if (status != null) {
            shipments = shipmentService.getShipmentsByStatus(status);
        } else {
            shipments = shipmentService.getAllShipments();
        }
        return ResponseEntity.ok(shipments);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getShipmentById(@PathVariable Long id) {
        try {
            Shipment shipment = shipmentService.getShipmentById(id);
            return ResponseEntity.ok(shipment);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createShipment(@Valid @RequestBody ShipmentRequest request) {
        try {
            Shipment shipment = shipmentService.createShipment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentRequest request) {
        try {
            Shipment shipment = shipmentService.updateShipment(id, request);
            return ResponseEntity.ok(shipment);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShipment(@PathVariable Long id) {
        try {
            shipmentService.deleteShipment(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Shipment deleted successfully");
            return ResponseEntity.ok(response);
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

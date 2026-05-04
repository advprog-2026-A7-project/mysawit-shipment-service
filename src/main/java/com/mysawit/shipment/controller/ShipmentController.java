package com.mysawit.shipment.controller;

import com.mysawit.shipment.dto.ShipmentRequest;
import com.mysawit.shipment.model.Shipment;
import com.mysawit.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.ok(shipmentService.getShipmentById(id));
        } catch (RuntimeException e) {
            return notFoundError(e);
        }
    }

    @PostMapping
    public ResponseEntity<?> createShipment(@Valid @RequestBody ShipmentRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.createShipment(request));
        } catch (RuntimeException e) {
            return badRequestError(e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateShipment(@PathVariable Long id, @Valid @RequestBody ShipmentRequest request) {
        try {
            return ResponseEntity.ok(shipmentService.updateShipment(id, request));
        } catch (RuntimeException e) {
            return notFoundError(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShipment(@PathVariable Long id) {
        try {
            shipmentService.deleteShipment(id);
            return ResponseEntity.ok(Map.of("message", "Shipment deleted successfully"));
        } catch (RuntimeException e) {
            return notFoundError(e);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "mysawit-shipment-service"));
    }

    private ResponseEntity<Map<String, String>> notFoundError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    private ResponseEntity<Map<String, String>> badRequestError(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

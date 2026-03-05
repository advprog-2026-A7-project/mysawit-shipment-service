package com.mysawit.shipment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ShipmentExceptionHandler {

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ShipmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ShipmentForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ShipmentForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildError("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(ShipmentInvalidTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(ShipmentInvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError("INVALID_STATUS_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGeneric(RuntimeException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    private Map<String, String> buildError(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}

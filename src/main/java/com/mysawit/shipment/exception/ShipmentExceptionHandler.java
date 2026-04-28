package com.mysawit.shipment.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(ShipmentWeightExceededException.class)
    public ResponseEntity<Map<String, String>> handleWeightExceeded(ShipmentWeightExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("WEIGHT_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(HarvestValidationException.class)
    public ResponseEntity<Map<String, String>> handleHarvestValidation(HarvestValidationException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(buildError("HARVEST_VALIDATION_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(HarvestServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleHarvestServiceUnavailable(HarvestServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildError("HARVEST_SERVICE_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(ObjectError::getDefaultMessage)
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError("BAD_REQUEST", message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGeneric(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError("INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
    }

    private Map<String, String> buildError(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}

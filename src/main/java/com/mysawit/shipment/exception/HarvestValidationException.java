package com.mysawit.shipment.exception;

import org.springframework.http.HttpStatus;

public class HarvestValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final HttpStatus status;

    public HarvestValidationException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public HarvestValidationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static HarvestValidationException notFound(String message) {
        return new HarvestValidationException(message, HttpStatus.NOT_FOUND);
    }

    public static HarvestValidationException badRequest(String message) {
        return new HarvestValidationException(message, HttpStatus.BAD_REQUEST);
    }

    public static HarvestValidationException conflict(String message) {
        return new HarvestValidationException(message, HttpStatus.CONFLICT);
    }

    public HttpStatus getStatus() {
        return status;
    }
}

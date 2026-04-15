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

    public HttpStatus getStatus() {
        return status;
    }
}

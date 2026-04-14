package com.mysawit.shipment.exception;

public class HarvestServiceUnavailableException extends RuntimeException {

    public HarvestServiceUnavailableException(String message) {
        super(message);
    }

    public HarvestServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

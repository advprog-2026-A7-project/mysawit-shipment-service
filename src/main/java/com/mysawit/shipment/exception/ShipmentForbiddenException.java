package com.mysawit.shipment.exception;

public class ShipmentForbiddenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShipmentForbiddenException(String message) {
        super(message);
    }
}

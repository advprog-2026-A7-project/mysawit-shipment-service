package com.mysawit.shipment.exception;

public class ShipmentInvalidTransitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShipmentInvalidTransitionException(String message) {
        super(message);
    }
}

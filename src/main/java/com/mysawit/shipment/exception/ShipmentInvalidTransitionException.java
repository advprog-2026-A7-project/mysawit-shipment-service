package com.mysawit.shipment.exception;

public class ShipmentInvalidTransitionException extends RuntimeException {
    public ShipmentInvalidTransitionException(String message) {
        super(message);
    }
}

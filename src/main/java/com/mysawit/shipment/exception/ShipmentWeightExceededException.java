package com.mysawit.shipment.exception;

public class ShipmentWeightExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShipmentWeightExceededException(String message) {
        super(message);
    }
}

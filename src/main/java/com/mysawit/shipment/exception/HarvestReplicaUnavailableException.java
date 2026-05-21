package com.mysawit.shipment.exception;

public class HarvestReplicaUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HarvestReplicaUnavailableException(String message) {
        super(message);
    }

    public HarvestReplicaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.mysawit.shipment.profiling;

record ShipmentFeatureProfileResult(
        String area,
        String feature,
        String entryPoint,
        boolean success,
        long durationMs,
        String notes
) {
    String status() {
        return success ? "PASS" : "FAIL";
    }
}

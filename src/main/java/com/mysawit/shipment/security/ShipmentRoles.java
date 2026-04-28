package com.mysawit.shipment.security;

import java.util.Set;

public final class ShipmentRoles {

    public static final String ADMIN = "ADMIN";
    public static final String MANDOR = "MANDOR";
    public static final String SUPIR = "SUPIR";
    public static final Set<String> ALLOWED_ROLES = Set.of(SUPIR, MANDOR, ADMIN);

    private ShipmentRoles() {
    }
}

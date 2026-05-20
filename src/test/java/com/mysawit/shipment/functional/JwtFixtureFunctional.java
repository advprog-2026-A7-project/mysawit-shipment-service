package com.mysawit.shipment.functional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public class JwtFixtureFunctional {
    private static final String SECRET = "test-functional-secret-key-minimum-32-characters-long";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String adminToken() {
        return generateToken(UUID.randomUUID().toString(), "ADMIN");
    }

    public static String mandorToken(UUID id) {
        return generateToken(id.toString(), "MANDOR");
    }

    public static String supirToken(UUID id) {
        return generateToken(id.toString(), "SUPIR");
    }

    private static String generateToken(String subject, String role) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(KEY)
                .compact();
    }
}

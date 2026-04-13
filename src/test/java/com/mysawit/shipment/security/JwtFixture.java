package com.mysawit.shipment.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public final class JwtFixture {

    public static final String TEST_SECRET = "test-secret-key-for-unit-tests-minimum-32-chars";

    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtFixture() {
    }

    public static String supirToken(String userId) {
        return buildToken(userId, "SUPIR");
    }

    public static String mandorToken(String userId) {
        return buildToken(userId, "MANDOR");
    }

    public static String adminToken(String userId) {
        return buildToken(userId, "ADMIN");
    }

    public static String tokenWithRole(String userId, String role) {
        return buildToken(userId, role);
    }

    public static String expiredToken(String userId, String role) {
        Date past = new Date(System.currentTimeMillis() - 100_000);
        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date(past.getTime() - 100_000))
                .expiration(past)
                .signWith(SIGNING_KEY)
                .compact();
    }

    private static String buildToken(String userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 86_400_000);
        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(SIGNING_KEY)
                .compact();
    }
}

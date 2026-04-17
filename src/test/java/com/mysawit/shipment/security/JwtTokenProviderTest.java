package com.mysawit.shipment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(JwtFixture.TEST_SECRET);
    }

    @Test
    void validateTokenReturnsTrueForValidToken() {
        String token = JwtFixture.supirToken(UUID.randomUUID().toString());

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("not-a-real-jwt"));
    }

    @Test
    void validateTokenReturnsFalseForExpiredToken() {
        String token = JwtFixture.expiredToken(UUID.randomUUID().toString(), "SUPIR");

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateTokenReturnsFalseForEmptyToken() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void getUserIdReturnsSubjectFromToken() {
        String userId = UUID.randomUUID().toString();
        String token = JwtFixture.supirToken(userId);

        assertEquals(userId, jwtTokenProvider.getUserId(token));
    }

    @Test
    void getRoleReturnsRoleClaimFromToken() {
        String token = JwtFixture.supirToken(UUID.randomUUID().toString());

        assertEquals("SUPIR", jwtTokenProvider.getRole(token));
    }

    @Test
    void getRoleReturnsMandorForMandorToken() {
        String token = JwtFixture.mandorToken(UUID.randomUUID().toString());

        assertEquals("MANDOR", jwtTokenProvider.getRole(token));
    }

    @Test
    void getClaimsReturnsAllClaims() {
        String userId = UUID.randomUUID().toString();
        String token = JwtFixture.supirToken(userId);

        var claims = jwtTokenProvider.getClaims(token);

        assertEquals(userId, claims.getSubject());
        assertEquals(userId, claims.get("userId", String.class));
        assertEquals("SUPIR", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
}

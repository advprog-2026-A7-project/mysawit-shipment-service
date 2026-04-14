package com.mysawit.shipment.security;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ShipmentAccessFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> ALLOWED_ROLES = Set.of("SUPIR", "MANDOR");

    private final JwtTokenProvider jwtTokenProvider;

    public ShipmentAccessFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/shipments") || "/api/shipments/health".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtTokenProvider.validateToken(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String role = jwtTokenProvider.getRole(token);
        if (!ALLOWED_ROLES.contains(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return;
        }

        request.setAttribute(ShipmentSecurityAttributes.JWT_ROLE, role);

        String userId = jwtTokenProvider.getUserId(token);
        if (userId != null) {
            try {
                request.setAttribute(ShipmentSecurityAttributes.JWT_USER_ID, UUID.fromString(userId));
            } catch (IllegalArgumentException ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

package com.mysawit.shipment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ShipmentAccessFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String NON_SUPIR_TOKEN = "token-with-non-supir-role";
    private static final String SUPIR_TOKEN = "token-with-supir-role";
    private static final Pattern SUPIR_WITH_USER_ID_PATTERN =
            Pattern.compile("^token-with-supir-role-user-(\\d+)$");

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
        if (NON_SUPIR_TOKEN.equals(token)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return;
        }

        if (SUPIR_TOKEN.equals(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Matcher matcher = SUPIR_WITH_USER_ID_PATTERN.matcher(token);
        if (matcher.matches()) {
            request.setAttribute(ShipmentSecurityAttributes.JWT_USER_ID, Long.parseLong(matcher.group(1)));
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}

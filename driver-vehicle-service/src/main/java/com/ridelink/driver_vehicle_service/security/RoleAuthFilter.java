package com.ridelink.driver_vehicle_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoleAuthFilter extends OncePerRequestFilter {

    private static final Set<String> VALID_ROLES = Set.of("DRIVER", "ADMIN", "SERVICE");
    private final ObjectMapper objectMapper;

    public RoleAuthFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui/") || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs/") || path.equals("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String role = request.getHeader("X-User-Role");
        if (role == null || role.isBlank()) {
            writeUnauthorized(response, "Missing X-User-Role header", request.getRequestURI());
            return;
        }
        role = role.trim();
        if (!VALID_ROLES.contains(role)) {
            writeUnauthorized(response, "Invalid X-User-Role header", request.getRequestURI());
            return;
        }

        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            writeUnauthorized(response, "Missing X-User-Id header", request.getRequestURI());
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                userId.trim(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message, String path)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpServletResponse.SC_UNAUTHORIZED,
                "error", "Unauthorized",
                "message", message,
                "path", path));
    }
}
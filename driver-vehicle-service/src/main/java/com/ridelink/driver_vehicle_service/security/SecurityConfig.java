package com.ridelink.driver_vehicle_service.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableMethodSecurity
@SecuritySchemes({
        @SecurityScheme(name = "userRoleHeader", type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER, paramName = "X-User-Role"),
        @SecurityScheme(name = "userIdHeader", type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER, paramName = "X-User-Id")
})
public class SecurityConfig {

        @Bean
        ObjectMapper objectMapper() {
                return new ObjectMapper();
        }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized", "Missing or invalid role headers", request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden", "Insufficient role", request.getRequestURI())))
                .addFilterBefore(new RoleAuthFilter(objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void writeError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String error,
            String message,
            String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", java.time.Instant.now().toString(),
                "status", status,
                "error", error,
                "message", message,
                "path", path));
    }
}
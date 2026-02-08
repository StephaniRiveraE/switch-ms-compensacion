package com.bancario.compensacion.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro de seguridad para validar requests provenientes del AWS API Gateway
 * (APIM).
 * 
 * El APIM inyecta un header secreto (x-origin-secret) en cada request para
 * garantizar
 * que solo traffic autorizado llegue al microservicio.
 * 
 * IMPORTANTE: Este filtro se DESACTIVA en desarrollo local
 * (apim.security.enabled=false)
 * y se ACTIVA en producción AWS (apim.security.enabled=true).
 */
@Slf4j
@Component
@Order(1)
public class ApimSecurityFilter implements Filter {

    @Value("${apim.origin.secret:}")
    private String expectedSecret;

    @Value("${apim.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Si la seguridad está deshabilitada (local), permitir todo
        if (!securityEnabled) {
            chain.doFilter(request, response);
            return;
        }

        // Permitir requests de health check sin validación
        String path = httpRequest.getRequestURI();
        if (path.contains("/actuator/health") || path.contains("/health")) {
            chain.doFilter(request, response);
            return;
        }

        // Validar header x-origin-secret
        String receivedSecret = httpRequest.getHeader("x-origin-secret");

        if (receivedSecret == null || !receivedSecret.equals(expectedSecret)) {
            log.warn("Request rejected - Invalid or missing x-origin-secret header. URI: {}", path);
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid origin\"}");
            return;
        }

        // Request válido, continuar
        chain.doFilter(request, response);
    }
}

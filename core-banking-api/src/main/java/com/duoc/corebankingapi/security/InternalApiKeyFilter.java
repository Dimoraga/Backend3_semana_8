package com.duoc.corebankingapi.security;

import com.duoc.corebankingapi.config.InternalApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * core-banking-api nunca es llamado por usuarios finales: solo por los 3
 * BFF. En vez de validar JWT de usuario aqui (eso ya lo hizo el BFF), se
 * exige un secreto compartido servicio-a-servicio en cada request a
 * /internal/**. Confianza de red/servicio, no autenticacion de usuario.
 */
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";

    private final InternalApiKeyProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/internal/")) {
            chain.doFilter(request, response);
            return;
        }

        String suministrada = request.getHeader(HEADER);
        String esperada = properties.getInternalApiKey();
        if (esperada == null || esperada.isBlank() || !esperada.equals(suministrada)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"X-Internal-Api-Key ausente o invalida\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

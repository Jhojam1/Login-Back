package com.proyecto.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.auth.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Respuesta que se devuelve cuando alguien intenta entrar a un recurso protegido
 * sin estar autenticado (token ausente, caducado o invalido).
 *
 * <p>Sin esta clase, Spring Security devolveria una pagina HTML de login, que es inutil
 * para un frontend React. Aqui se fuerza un JSON con el mismo formato de error que usa
 * el resto de la API.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Escribe la respuesta 401 en formato JSON.
     *
     * @param request       peticion rechazada
     * @param response      respuesta a escribir
     * @param authException causa del rechazo
     * @throws IOException si falla la escritura en el flujo de salida
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code("UNAUTHENTICATED")
                .message("Necesitas iniciar sesion para acceder a este recurso")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

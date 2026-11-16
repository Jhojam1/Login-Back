package com.proyecto.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

/**
 * Respuesta a la consulta de sesion de un usuario.
 *
 * @param username       nombre consultado, tal como se recibio
 * @param exists         si existe una cuenta con ese nombre o correo
 * @param loggedIn       si tiene al menos una sesion abierta
 * @param activeSessions numero de sesiones abiertas (puede haber varias: movil, portatil...)
 * @param lastLoginAt    fecha del ultimo inicio de sesion correcto
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estado de sesion de un usuario")
public record SessionStatusResponse(

        String username,

        @Schema(description = "false si no hay ninguna cuenta con ese nombre")
        boolean exists,

        @Schema(description = "true si tiene al menos una sesion sin cerrar ni caducar")
        boolean loggedIn,

        @Schema(description = "Numero de dispositivos con sesion abierta")
        long activeSessions,

        Instant lastLoginAt
) {

    /**
     * Construye la respuesta para un usuario que no existe.
     *
     * @param username nombre consultado
     * @return respuesta con {@code exists} y {@code loggedIn} en falso
     */
    public static SessionStatusResponse notFound(String username) {
        return SessionStatusResponse.builder()
                .username(username)
                .exists(false)
                .loggedIn(false)
                .activeSessions(0)
                .build();
    }
}

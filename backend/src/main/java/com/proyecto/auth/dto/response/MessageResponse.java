package com.proyecto.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta generica para operaciones que no devuelven datos (logout, borrados, etc.).
 *
 * @param message texto descriptivo del resultado
 */
@Schema(description = "Mensaje simple de confirmacion")
public record MessageResponse(String message) {

    /**
     * Atajo para construir la respuesta sin escribir {@code new} en cada controlador.
     *
     * @param message texto a devolver
     * @return la respuesta lista para serializar
     */
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}

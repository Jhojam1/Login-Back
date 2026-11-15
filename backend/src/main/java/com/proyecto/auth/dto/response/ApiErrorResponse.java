package com.proyecto.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Formato unico de error para toda la API.
 *
 * <p>Que todos los errores tengan la misma forma permite que el frontend escriba
 * un solo manejador en lugar de adivinar la estructura en cada endpoint.</p>
 *
 * @param timestamp momento en que se produjo el error
 * @param status    codigo HTTP
 * @param error     nombre corto del codigo HTTP
 * @param code      codigo interno estable para que el frontend reaccione sin leer el texto
 * @param message   mensaje legible para la persona usuaria
 * @param path      ruta que provoco el error
 * @param details   errores de validacion campo a campo, si los hubiera
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estructura comun de todos los errores de la API")
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<Map<String, String>> details
) {
}

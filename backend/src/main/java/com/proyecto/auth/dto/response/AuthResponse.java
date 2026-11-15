package com.proyecto.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Respuesta de un inicio de sesion o de un refresco correcto.
 *
 * @param accessToken  JWT de vida corta que el frontend envia en cada peticion
 * @param refreshToken token opaco de vida larga para renovar el acceso sin volver a loguearse
 * @param tokenType    esquema de autenticacion; siempre {@code Bearer}
 * @param expiresIn    segundos que le quedan de vida al token de acceso
 * @param user         datos del usuario autenticado, para pintar la interfaz sin otra llamada
 */
@Builder
@Schema(description = "Tokens emitidos tras una autenticacion correcta")
public record AuthResponse(

        @Schema(description = "JWT a enviar en la cabecera Authorization")
        String accessToken,

        @Schema(description = "Token para renovar el acceso cuando el JWT expire")
        String refreshToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos de validez del token de acceso", example = "900")
        long expiresIn,

        UserResponse user
) {
}

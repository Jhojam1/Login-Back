package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Peticion para canjear un token de refresco por un nuevo token de acceso.
 *
 * @param refreshToken valor del token de refresco entregado en el login
 */
@Schema(description = "Token de refresco a canjear")
public record RefreshTokenRequest(

        @NotBlank(message = "El token de refresco es obligatorio")
        String refreshToken
) {
}

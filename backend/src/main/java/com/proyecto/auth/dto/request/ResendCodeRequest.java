package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Peticion para que se vuelva a enviar el codigo de verificacion.
 *
 * @param challengeId identificador del intento de inicio de sesion en curso
 */
@Schema(description = "Reenvio del codigo de verificacion")
public record ResendCodeRequest(

        @NotBlank(message = "Falta el identificador de la verificacion")
        String challengeId
) {
}

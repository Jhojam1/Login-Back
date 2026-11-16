package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Segundo paso del inicio de sesion: el codigo recibido por correo.
 *
 * @param challengeId identificador que devolvio {@code /auth/login}
 * @param code        codigo numerico que escribio la persona
 */
@Schema(description = "Codigo de verificacion recibido por correo")
public record VerifyCodeRequest(

        @NotBlank(message = "Falta el identificador de la verificacion")
        String challengeId,

        @Schema(example = "483920")
        @NotBlank(message = "Escribe el codigo que recibiste")
        @Pattern(regexp = "^[0-9]{4,10}$", message = "El codigo solo puede tener digitos")
        String code
) {
}

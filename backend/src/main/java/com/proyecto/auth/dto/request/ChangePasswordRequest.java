package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Peticion de cambio de contrasena del propio usuario autenticado.
 *
 * @param currentPassword contrasena actual, exigida para evitar que alguien
 *                        con la sesion abierta secuestre la cuenta
 * @param newPassword     contrasena nueva, sujeta a la misma politica que en el registro
 */
@Schema(description = "Cambio de contrasena del usuario autenticado")
public record ChangePasswordRequest(

        @NotBlank(message = "Debes indicar tu contrasena actual")
        String currentPassword,

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).+$",
                message = "La contrasena debe incluir al menos una minuscula, una mayuscula y un numero"
        )
        String newPassword
) {
}

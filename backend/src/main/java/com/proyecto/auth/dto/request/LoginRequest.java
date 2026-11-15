package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales enviadas por el formulario de inicio de sesion.
 *
 * @param identifier nombre de usuario o correo electronico
 * @param password   contrasena en texto plano (viaja cifrada por HTTPS)
 */
@Schema(description = "Credenciales de acceso")
public record LoginRequest(

        @Schema(example = "admin", description = "Nombre de usuario o correo electronico")
        @NotBlank(message = "Indica tu usuario o correo")
        String identifier,

        @Schema(example = "Admin.2026")
        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}

package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Datos modificables de un usuario.
 *
 * <p>Los campos nulos se interpretan como "no cambiar", lo que permite usar el mismo
 * DTO para actualizaciones parciales sin multiplicar endpoints.</p>
 *
 * @param firstName nuevo nombre, o {@code null} para conservarlo
 * @param lastName  nuevo apellido, o {@code null} para conservarlo
 * @param email     nuevo correo, o {@code null} para conservarlo
 * @param enabled   nuevo estado de la cuenta, o {@code null} para conservarlo
 */
@Schema(description = "Campos editables de un usuario; los nulos no se modifican")
public record UpdateUserRequest(

        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String firstName,

        @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
        String lastName,

        @Email(message = "El formato del correo no es valido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        Boolean enabled
) {
}

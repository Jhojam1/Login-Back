package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que envia el formulario de registro.
 *
 * <p>Se usa un {@code record} porque un DTO de entrada debe ser inmutable: una vez
 * validado, nadie deberia poder modificarlo durante el procesamiento de la peticion.
 * Las anotaciones de validacion hacen que Spring rechace la peticion con un 400
 * antes de que el controlador se ejecute, manteniendo la logica de negocio limpia.</p>
 *
 * @param username  nombre de usuario unico (3-50 caracteres alfanumericos)
 * @param email     correo electronico valido y unico
 * @param password  contrasena en texto plano; se cifra con BCrypt antes de guardarla
 * @param firstName nombre de la persona
 * @param lastName  apellido de la persona
 */
@Schema(description = "Datos necesarios para crear una cuenta nueva")
public record RegisterRequest(

        @Schema(example = "jhojam")
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre de usuario solo admite letras, numeros, punto, guion y guion bajo")
        String username,

        @Schema(example = "jhojam@correo.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo no es valido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @Schema(example = "Clave.Segura2026")
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).+$",
                message = "La contrasena debe incluir al menos una minuscula, una mayuscula y un numero"
        )
        String password,

        @Schema(example = "Jhojam")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String firstName,

        @Schema(example = "Caraballo")
        @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
        String lastName
) {
}

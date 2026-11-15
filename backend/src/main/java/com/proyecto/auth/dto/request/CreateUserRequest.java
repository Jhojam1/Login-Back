package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Datos para que un administrador cree una cuenta desde el panel.
 *
 * <p>Se diferencia de {@code RegisterRequest} en un punto importante: aqui SI se pueden
 * indicar los roles. El registro publico nunca permite elegir rol (siempre otorga
 * {@code ROLE_USER}); si ambos casos compartieran DTO, cualquiera podria enviar
 * {@code roles: ["ROLE_ADMIN"]} al formulario de registro y hacerse administrador.</p>
 *
 * @param username  nombre de usuario unico
 * @param email     correo electronico unico
 * @param password  contrasena inicial; la persona deberia cambiarla al entrar
 * @param firstName nombre
 * @param lastName  apellido
 * @param roles     roles a asignar; si llega vacio se otorga {@code ROLE_USER}
 */
@Schema(description = "Datos para crear un usuario desde el panel de administracion")
public record CreateUserRequest(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre de usuario solo admite letras, numeros, punto, guion y guion bajo")
        String username,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo no es valido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).+$",
                message = "La contrasena debe incluir al menos una minuscula, una mayuscula y un numero"
        )
        String password,

        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String firstName,

        @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
        String lastName,

        @Schema(example = "[\"ROLE_USER\"]")
        Set<String> roles
) {
}

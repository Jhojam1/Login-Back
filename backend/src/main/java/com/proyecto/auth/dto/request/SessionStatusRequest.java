package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Peticion para consultar si un usuario tiene la sesion iniciada.
 *
 * <p>La envia otro microservicio que solo necesita saber si alguien esta conectado,
 * sin disponer de su token.</p>
 *
 * <p><b>Esto NO sirve para autorizar.</b> Saber que una persona tiene una sesion
 * abierta en algun sitio no demuestra que sea ella quien esta haciendo la peticion:
 * cualquiera puede escribir un nombre de usuario aqui. Para decidir si se permite una
 * operacion hay que usar {@code /introspect} con el token de quien la solicita.</p>
 *
 * @param username nombre de usuario o correo a consultar
 */
@Schema(description = "Usuario del que se quiere saber si tiene sesion iniciada")
public record SessionStatusRequest(

        @Schema(example = "jhojam", description = "Nombre de usuario o correo")
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String username
) {
}

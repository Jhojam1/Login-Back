package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Peticion de introspeccion: la envia OTRO microservicio (por ejemplo el que gestiona
 * los archivos en S3) para preguntarle a este servicio si un token sigue siendo valido
 * y que puede hacer su portador.
 *
 * <p>Sigue el espiritu del estandar RFC 7662 (OAuth 2.0 Token Introspection).</p>
 *
 * @param token token de acceso (JWT) que se quiere verificar
 */
@Schema(description = "Token de acceso que un servicio externo quiere validar")
public record IntrospectTokenRequest(

        @NotBlank(message = "El token a validar es obligatorio")
        String token
) {
}

package com.proyecto.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;

/**
 * Respuesta de introspeccion de token.
 *
 * <p><b>Este es el contrato que consumen los otros equipos.</b> Cuando el microservicio
 * de archivos (S3) recibe una peticion con un JWT, llama a
 * {@code POST /api/v1/auth/introspect} y recibe este objeto para decidir si deja pasar
 * la peticion y con que permisos.</p>
 *
 * <p>Los campos nulos se omiten del JSON ({@code NON_NULL}): si el token no es valido
 * la respuesta se reduce a {@code {"active": false, "reason": "..."}}, sin filtrar
 * ninguna informacion del usuario.</p>
 *
 * @param active      unico campo siempre presente: indica si el token es utilizable
 * @param reason      motivo del rechazo cuando {@code active} es falso
 * @param userId      identificador del usuario dueno del token
 * @param username    nombre de usuario
 * @param email       correo del usuario
 * @param roles       roles vigentes del usuario
 * @param permissions permisos efectivos del usuario
 * @param issuedAt    instante de emision del token
 * @param expiresAt   instante de expiracion del token
 * @param issuer      emisor del token (este servicio)
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resultado de validar un token desde otro microservicio")
public record IntrospectTokenResponse(

        @Schema(description = "true si el token es valido, no ha expirado y el usuario sigue activo")
        boolean active,

        @Schema(description = "Motivo del rechazo cuando active=false", example = "TOKEN_EXPIRED")
        String reason,

        Long userId,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions,
        Instant issuedAt,
        Instant expiresAt,
        String issuer
) {

    /**
     * Construye una respuesta negativa, sin datos del usuario.
     *
     * @param reason codigo del motivo, por ejemplo {@code TOKEN_EXPIRED} o {@code USER_DISABLED}
     * @return respuesta con {@code active = false}
     */
    public static IntrospectTokenResponse inactive(String reason) {
        return IntrospectTokenResponse.builder()
                .active(false)
                .reason(reason)
                .build();
    }
}

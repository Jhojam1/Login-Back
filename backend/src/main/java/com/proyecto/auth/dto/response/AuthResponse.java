package com.proyecto.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Respuesta del inicio de sesion. Tiene dos formas segun este activada o no la
 * verificacion en dos pasos.
 *
 * <p><b>Sin verificacion</b> (o ya superada): llegan los tokens y
 * {@code mfaRequired} es falso.</p>
 *
 * <p><b>Con verificacion pendiente</b>: NO llega ningun token. Solo
 * {@code mfaRequired: true} y el {@code challengeId} con el que continuar en
 * {@code /auth/verify-code}. Es importante que los tokens no viajen todavia: si lo
 * hicieran, bastaria con ignorar la pantalla del codigo para entrar igualmente.</p>
 *
 * @param accessToken    JWT de acceso; ausente si falta verificar
 * @param refreshToken   token de refresco; ausente si falta verificar
 * @param tokenType      esquema de autenticacion, siempre {@code Bearer}
 * @param expiresIn      segundos de validez del token de acceso
 * @param user           datos del usuario; ausentes si falta verificar
 * @param mfaRequired    {@code true} si hay que introducir un codigo para continuar
 * @param challengeId    identificador del intento, necesario para verificar el codigo
 * @param maskedEmail    correo parcialmente tapado, para decir a donde se mando el codigo
 * @param codeExpiresInSeconds segundos que dura el codigo enviado
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resultado del inicio de sesion")
public record AuthResponse(

        @Schema(description = "JWT a enviar en la cabecera Authorization")
        String accessToken,

        @Schema(description = "Token para renovar el acceso cuando el JWT expire")
        String refreshToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos de validez del token de acceso", example = "900")
        Long expiresIn,

        UserResponse user,

        @Schema(description = "true si hay que introducir el codigo enviado por correo")
        boolean mfaRequired,

        @Schema(description = "Identificador del intento, para /auth/verify-code")
        String challengeId,

        @Schema(example = "jh***@correo.com")
        String maskedEmail,

        @Schema(description = "Segundos que dura el codigo", example = "600")
        Long codeExpiresInSeconds
) {
}

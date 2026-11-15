package com.proyecto.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;

/**
 * Representacion publica de un usuario.
 *
 * <p>Es deliberadamente distinta de la entidad {@code User}: aqui NO existe el campo
 * {@code password}. Exponer entidades JPA directamente en la API es uno de los errores
 * mas comunes y filtra datos sensibles, ademas de atar el contrato HTTP al modelo de
 * base de datos.</p>
 *
 * @param id          identificador del usuario
 * @param username    nombre de usuario
 * @param email       correo electronico
 * @param firstName   nombre
 * @param lastName    apellido
 * @param fullName    nombre completo listo para mostrar
 * @param enabled     si la cuenta esta activa
 * @param locked      si la cuenta esta bloqueada
 * @param roles       nombres de los roles asignados
 * @param permissions permisos efectivos derivados de esos roles
 * @param createdAt   fecha de alta
 * @param lastLoginAt fecha del ultimo acceso correcto
 */
@Builder
@Schema(description = "Datos publicos de un usuario")
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        boolean enabled,
        boolean locked,
        Set<String> roles,
        Set<String> permissions,
        Instant createdAt,
        Instant lastLoginAt
) {
}

package com.proyecto.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Set;

/**
 * Representacion publica de un rol y los permisos que concede.
 *
 * @param id          identificador del rol
 * @param name        nombre del rol
 * @param description descripcion legible
 * @param permissions permisos incluidos
 */
@Builder
@Schema(description = "Rol con su lista de permisos")
public record RoleResponse(
        Long id,
        String name,
        String description,
        Set<String> permissions
) {
}

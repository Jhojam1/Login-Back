package com.proyecto.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Representacion publica de un permiso.
 *
 * @param id          identificador del permiso
 * @param name        nombre del permiso
 * @param description descripcion legible
 * @param uiOnly      {@code true} si solo decide que muestra la interfaz y no protege
 *                    ninguna operacion del servidor
 */
@Builder
@Schema(description = "Permiso del catalogo")
public record PermissionResponse(
        Long id,
        String name,
        String description,

        @Schema(description = "true si el permiso solo afecta a la interfaz")
        boolean uiOnly
) {
}

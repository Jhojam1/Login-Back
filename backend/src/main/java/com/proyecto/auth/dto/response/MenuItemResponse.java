package com.proyecto.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Entrada del menu tal como la recibe el frontend.
 *
 * @param id                 identificador de la entrada
 * @param label              texto visible
 * @param path               ruta a la que lleva
 * @param icon               nombre del icono
 * @param requiredPermission permiso necesario, o {@code null} si no exige ninguno
 * @param displayOrder       posicion en el menu
 * @param visible            si esta activa
 * @param systemItem         si la creo el sistema y por tanto no se puede eliminar
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Entrada del menu de navegacion")
public record MenuItemResponse(
        Long id,
        String label,
        String path,
        String icon,
        String requiredPermission,
        int displayOrder,
        boolean visible,
        boolean systemItem
) {
}

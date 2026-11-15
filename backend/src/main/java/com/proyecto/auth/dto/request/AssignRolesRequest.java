package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * Peticion del panel de administracion para reemplazar los roles de un usuario.
 *
 * @param roles conjunto de nombres de rol, por ejemplo {@code ["ROLE_USER","ROLE_MANAGER"]}
 */
@Schema(description = "Roles que quedaran asignados al usuario")
public record AssignRolesRequest(

        @Schema(example = "[\"ROLE_USER\", \"ROLE_MANAGER\"]")
        @NotEmpty(message = "Debes indicar al menos un rol")
        Set<String> roles
) {
}

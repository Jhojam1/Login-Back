package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un permiso de solo interfaz desde el panel.
 *
 * <p>No existe un campo para decidir si el permiso se verifica en el backend: los
 * creados por aqui son siempre de solo interfaz. Permitir lo contrario seria enganoso,
 * porque el servidor no tiene ninguna comprobacion asociada a un nombre que acaba de
 * aparecer en la base de datos.</p>
 *
 * @param name        nombre del permiso; se normaliza a mayusculas
 * @param description para que sirve, en lenguaje llano
 */
@Schema(description = "Permiso de solo interfaz a crear")
public record PermissionRequest(

        @Schema(example = "REPORT_VIEW")
        @NotBlank(message = "El nombre del permiso es obligatorio")
        @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$",
                message = "El nombre debe empezar por una letra y solo admite letras, numeros y guion bajo")
        String name,

        @Schema(example = "Ver la seccion de reportes")
        @Size(max = 180, message = "La descripcion no puede superar 180 caracteres")
        String description
) {

    /**
     * Devuelve el nombre normalizado tal como se guarda y viaja en el token.
     *
     * @return el nombre en mayusculas y sin espacios alrededor
     */
    public String normalizedName() {
        return name.trim().toUpperCase();
    }
}

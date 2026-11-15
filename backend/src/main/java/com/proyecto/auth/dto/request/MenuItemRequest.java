package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o modificar una entrada del menu.
 *
 * @param label              texto visible en el menu
 * @param path               ruta de la aplicacion a la que lleva
 * @param icon               nombre del icono a mostrar
 * @param requiredPermission permiso necesario para verla, o vacio si la ve cualquiera
 * @param displayOrder       posicion en el menu
 * @param visible            si se muestra o queda oculta sin borrarla
 */
@Schema(description = "Entrada del menu de navegacion")
public record MenuItemRequest(

        @Schema(example = "Reportes")
        @NotBlank(message = "El texto del menu es obligatorio")
        @Size(max = 60, message = "El texto no puede superar 60 caracteres")
        String label,

        @Schema(example = "/panel/reportes")
        @NotBlank(message = "La ruta es obligatoria")
        @Size(max = 180, message = "La ruta no puede superar 180 caracteres")
        @Pattern(regexp = "^/[a-zA-Z0-9/_-]*$",
                message = "La ruta debe empezar por / y solo admite letras, numeros, guion y guion bajo")
        String path,

        @Schema(example = "chart", description = "Nombre del icono del catalogo del frontend")
        @Size(max = 40, message = "El nombre del icono no puede superar 40 caracteres")
        @Pattern(regexp = "^[a-z-]*$", message = "El icono solo admite minusculas y guion")
        String icon,

        @Schema(example = "REPORT_VIEW", description = "Dejalo vacio para que la vea cualquier persona")
        @Size(max = 60, message = "El nombre del permiso no puede superar 60 caracteres")
        String requiredPermission,

        @PositiveOrZero(message = "La posicion no puede ser negativa")
        int displayOrder,

        Boolean visible
) {
}

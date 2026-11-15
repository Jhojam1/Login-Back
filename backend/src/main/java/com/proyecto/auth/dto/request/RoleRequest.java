package com.proyecto.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Datos para crear o modificar un rol y el conjunto de permisos que concede.
 *
 * <p>Los permisos se eligen del catalogo existente: no se inventan aqui. Un permiso solo
 * tiene efecto si hay codigo en algun servicio que lo compruebe, asi que crear nombres
 * nuevos desde el panel no daria ninguna capacidad real. Lo que si es util, y es lo que
 * permite este DTO, es combinar los permisos existentes en roles a medida.</p>
 *
 * @param name        nombre del rol; se normaliza al prefijo {@code ROLE_}
 * @param description texto legible que explica para quien es el rol
 * @param permissions permisos del catalogo que concedera el rol
 */
@Schema(description = "Rol a crear o modificar, con los permisos que concede")
public record RoleRequest(

        @Schema(example = "ROLE_AUDITOR", description = "Si omites el prefijo ROLE_, se anade solo")
        @NotBlank(message = "El nombre del rol es obligatorio")
        @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
        @Pattern(regexp = "^[A-Za-z_]+$",
                message = "El nombre del rol solo admite letras y guion bajo")
        String name,

        @Schema(example = "Puede consultar usuarios y archivos sin modificarlos")
        @Size(max = 180, message = "La descripcion no puede superar 180 caracteres")
        String description,

        @Schema(example = "[\"USER_READ\", \"FILE_READ\"]")
        @NotEmpty(message = "El rol debe conceder al menos un permiso")
        Set<String> permissions
) {

    /**
     * Devuelve el nombre del rol con el prefijo que exige Spring Security.
     *
     * <p>Sin el prefijo {@code ROLE_}, la expresion {@code hasRole('AUDITOR')} nunca
     * coincidiria. Normalizarlo aqui evita que un despiste al escribir el formulario
     * cree un rol que no funciona.</p>
     *
     * @return el nombre en mayusculas y con prefijo, por ejemplo {@code ROLE_AUDITOR}
     */
    public String normalizedName() {
        String upper = name.trim().toUpperCase();
        return upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
    }
}

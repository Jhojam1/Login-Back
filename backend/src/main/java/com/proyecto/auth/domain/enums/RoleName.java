package com.proyecto.auth.domain.enums;

import java.util.Set;

import static com.proyecto.auth.domain.enums.PermissionName.*;

/**
 * Catálogo de roles del sistema junto con los permisos que cada uno otorga.
 *
 * <p>El rol es el "paquete" que se le asigna a una persona; el permiso es la acción
 * concreta que el código verifica. Declarar la relación rol → permisos aquí mantiene
 * una única fuente de verdad que el sembrador de datos replica en la base de datos.</p>
 */
public enum RoleName {

    /** Control total del sistema: usuarios, roles, permisos y archivos. */
    ROLE_ADMIN(Set.of(
            USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE,
            ROLE_READ, ROLE_WRITE, ROLE_ASSIGN,
            PERMISSION_READ, PERMISSION_WRITE,
            MENU_WRITE,
            FILE_UPLOAD, FILE_READ, FILE_DELETE)),

    /** Gestiona usuarios y archivos, pero no puede redefinir roles ni permisos. */
    ROLE_MANAGER(Set.of(
            USER_READ, USER_CREATE, USER_UPDATE,
            ROLE_READ,
            FILE_UPLOAD, FILE_READ)),

    /** Rol por defecto de cualquier usuario registrado: acceso mínimo. */
    ROLE_USER(Set.of(
            USER_READ,
            FILE_UPLOAD, FILE_READ));

    /** Permisos que este rol concede. */
    private final Set<PermissionName> permissions;

    RoleName(Set<PermissionName> permissions) {
        this.permissions = permissions;
    }

    /**
     * Devuelve los permisos asociados al rol.
     *
     * @return conjunto inmutable de permisos
     */
    public Set<PermissionName> getPermissions() {
        return permissions;
    }

    /**
     * Devuelve el nombre del rol tal como lo espera Spring Security
     * (debe conservar el prefijo {@code ROLE_} para que funcione {@code hasRole}).
     *
     * @return identificador textual del rol, por ejemplo {@code ROLE_ADMIN}
     */
    public String getAuthority() {
        return name();
    }
}

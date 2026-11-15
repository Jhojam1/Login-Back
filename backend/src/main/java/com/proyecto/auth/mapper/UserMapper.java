package com.proyecto.auth.mapper;

import com.proyecto.auth.domain.entity.MenuItem;
import com.proyecto.auth.domain.entity.Permission;
import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.dto.response.MenuItemResponse;
import com.proyecto.auth.dto.response.PermissionResponse;
import com.proyecto.auth.dto.response.RoleResponse;
import com.proyecto.auth.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traductor entre las entidades de dominio y los DTOs que viajan por HTTP.
 *
 * <p>Aislar la conversion en una clase propia evita que los servicios se llenen de
 * codigo de mapeo y garantiza que la contrasena nunca acabe accidentalmente en una
 * respuesta: aqui simplemente no se copia.</p>
 */
@Component
public class UserMapper {

    /**
     * Convierte un usuario de dominio en su representacion publica.
     *
     * @param user entidad a convertir
     * @return DTO sin datos sensibles, con roles y permisos ya aplanados
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .locked(user.isAccountLocked())
                .roles(extractRoleNames(user))
                .permissions(extractPermissionNames(user))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Convierte un rol en su representacion publica, incluyendo sus permisos.
     *
     * @param role entidad a convertir
     * @return DTO del rol
     */
    public RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Convierte un permiso en su representacion publica.
     *
     * @param permission entidad a convertir
     * @return DTO del permiso
     */
    public PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .uiOnly(permission.isUiOnly())
                .build();
    }

    /**
     * Convierte una entrada del menu en su representacion publica.
     *
     * @param item entidad a convertir
     * @return DTO de la entrada
     */
    public MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .label(item.getLabel())
                .path(item.getPath())
                .icon(item.getIcon())
                .requiredPermission(item.getRequiredPermission())
                .displayOrder(item.getDisplayOrder())
                .visible(item.isVisible())
                .systemItem(item.isSystemItem())
                .build();
    }

    /**
     * Obtiene los nombres de los roles de un usuario.
     *
     * @param user usuario a inspeccionar
     * @return conjunto de nombres de rol
     */
    public Set<String> extractRoleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Obtiene los permisos efectivos de un usuario, es decir la union de los permisos
     * de todos sus roles, sin repeticiones.
     *
     * @param user usuario a inspeccionar
     * @return conjunto de nombres de permiso
     */
    public Set<String> extractPermissionNames(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}

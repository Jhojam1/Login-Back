package com.proyecto.auth.service.impl;

import com.proyecto.auth.domain.entity.Permission;
import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.enums.PermissionName;
import com.proyecto.auth.domain.enums.RoleName;
import com.proyecto.auth.dto.request.PermissionRequest;
import com.proyecto.auth.dto.request.RoleRequest;
import com.proyecto.auth.dto.response.PermissionResponse;
import com.proyecto.auth.dto.response.RoleResponse;
import com.proyecto.auth.exception.BusinessException;
import com.proyecto.auth.exception.DuplicateResourceException;
import com.proyecto.auth.exception.ResourceNotFoundException;
import com.proyecto.auth.mapper.UserMapper;
import com.proyecto.auth.repository.MenuItemRepository;
import com.proyecto.auth.repository.PermissionRepository;
import com.proyecto.auth.repository.RoleRepository;
import com.proyecto.auth.repository.UserRepository;
import com.proyecto.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementacion de la gestion de roles y la consulta de permisos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    /**
     * Roles que trae el sistema de serie y que no se pueden borrar.
     *
     * <p>Borrar {@code ROLE_USER} romperia el registro publico, que lo asigna a cada
     * cuenta nueva; borrar {@code ROLE_ADMIN} podria dejar el sistema sin nadie capaz de
     * administrarlo. Se protegen aqui en lugar de confiar en que nadie lo intente.</p>
     */
    private static final Set<String> SYSTEM_ROLES = Arrays.stream(RoleName.values())
            .map(RoleName::getAuthority)
            .collect(Collectors.toUnmodifiableSet());

    /** Permisos que el backend verifica y que por tanto no se gestionan desde el panel. */
    private static final Set<String> BACKEND_PERMISSIONS = Arrays.stream(PermissionName.values())
            .map(PermissionName::getAuthority)
            .collect(Collectors.toUnmodifiableSet());

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Lista todos los roles ordenados por nombre, con sus permisos incluidos.
     *
     * @return lista de roles
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findAllRoles() {
        return roleRepository.findAll(Sort.by("name")).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Crea un rol nuevo con los permisos indicados.
     *
     * @param request nombre, descripcion y permisos del rol
     * @return el rol creado
     * @throws DuplicateResourceException si ya existe un rol con ese nombre
     */
    @Override
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        String name = request.normalizedName();

        if (roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("nombre de rol", name);
        }

        Role role = Role.builder()
                .name(name)
                .description(StringUtils.hasText(request.description())
                        ? request.description()
                        : "Rol personalizado")
                .permissions(resolvePermissions(request.permissions()))
                .build();

        Role saved = roleRepository.save(role);
        log.info("Rol {} creado con {} permisos", name, saved.getPermissions().size());

        return userMapper.toResponse(saved);
    }

    /**
     * Actualiza la descripcion y los permisos de un rol.
     *
     * <p>El nombre no se cambia a proposito: el nombre del rol puede estar escrito en
     * anotaciones {@code @PreAuthorize} del codigo y en los permisos de otros servicios,
     * asi que renombrarlo desde el panel rompería silenciosamente esas comprobaciones.
     * Para "renombrar" se crea uno nuevo y se reasignan los usuarios.</p>
     *
     * @param id      identificador del rol
     * @param request nuevos datos del rol
     * @return el rol actualizado
     * @throws ResourceNotFoundException si el rol no existe
     */
    @Override
    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = getRoleOrThrow(id);

        if (StringUtils.hasText(request.description())) {
            role.setDescription(request.description());
        }
        role.setPermissions(resolvePermissions(request.permissions()));

        Role saved = roleRepository.save(role);
        log.info("Rol {} actualizado: ahora concede {} permisos",
                saved.getName(), saved.getPermissions().size());

        return userMapper.toResponse(saved);
    }

    /**
     * Elimina un rol personalizado.
     *
     * @param id identificador del rol a borrar
     * @throws BusinessException si es un rol del sistema o si hay usuarios que lo tienen
     */
    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleOrThrow(id);

        if (SYSTEM_ROLES.contains(role.getName())) {
            throw new BusinessException(HttpStatus.CONFLICT, "SYSTEM_ROLE",
                    "El rol %s forma parte del sistema y no se puede eliminar".formatted(role.getName()));
        }

        long usersWithRole = userRepository.countByRolesId(id);
        if (usersWithRole > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "ROLE_IN_USE",
                    "No se puede eliminar: %d usuario(s) tienen este rol. Quitaselo primero."
                            .formatted(usersWithRole));
        }

        roleRepository.delete(role);
        log.info("Rol {} eliminado", role.getName());
    }

    /**
     * Lista el catalogo completo de permisos ordenado por nombre.
     *
     * @return lista de permisos
     */
    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAll(Sort.by("name")).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Crea un permiso de solo interfaz.
     *
     * <p>Se rechazan los nombres que ya usa el enum {@code PermissionName}: esos tienen
     * comprobaciones reales en el servidor y permitir crear un homonimo "de interfaz"
     * confundiria dos cosas que deben quedar separadas.</p>
     *
     * @param request nombre y descripcion del permiso
     * @return el permiso creado
     * @throws DuplicateResourceException si el nombre ya existe en el catalogo
     * @throws BusinessException          si el nombre coincide con un permiso del backend
     */
    @Override
    @Transactional
    public PermissionResponse createUiPermission(PermissionRequest request) {
        String name = request.normalizedName();

        if (BACKEND_PERMISSIONS.contains(name)) {
            throw new BusinessException(HttpStatus.CONFLICT, "RESERVED_PERMISSION",
                    "%s es un permiso del backend y ya existe".formatted(name));
        }
        if (permissionRepository.existsByName(name)) {
            throw new DuplicateResourceException("nombre de permiso", name);
        }

        Permission permission = permissionRepository.save(Permission.builder()
                .name(name)
                .description(StringUtils.hasText(request.description())
                        ? request.description()
                        : "Permiso de interfaz")
                .uiOnly(true)
                .build());

        log.info("Permiso de solo interfaz creado: {}", name);

        return userMapper.toResponse(permission);
    }

    /**
     * Elimina un permiso de solo interfaz.
     *
     * <p>Se comprueba antes que no lo use ningun rol ni ninguna entrada del menu: borrarlo
     * a ciegas quitaria accesos o volveria invisible una seccion sin dejar rastro de por
     * que ha pasado.</p>
     *
     * @param id identificador del permiso
     * @throws BusinessException si es del backend o si algo lo esta usando
     */
    @Override
    @Transactional
    public void deleteUiPermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso", "id", id));

        if (!permission.isUiOnly()) {
            throw new BusinessException(HttpStatus.CONFLICT, "BACKEND_PERMISSION",
                    "%s lo verifica el backend y no se puede eliminar desde el panel"
                            .formatted(permission.getName()));
        }

        long rolesUsing = roleRepository.countByPermissionsId(id);
        if (rolesUsing > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "PERMISSION_IN_USE",
                    "No se puede eliminar: %d rol(es) lo conceden. Quitalo de esos roles primero."
                            .formatted(rolesUsing));
        }

        long menuItemsUsing = menuItemRepository.countByRequiredPermission(permission.getName());
        if (menuItemsUsing > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "PERMISSION_IN_USE",
                    "No se puede eliminar: %d entrada(s) del menu lo exigen."
                            .formatted(menuItemsUsing));
        }

        permissionRepository.delete(permission);
        log.info("Permiso de solo interfaz eliminado: {}", permission.getName());
    }

    /**
     * Traduce un conjunto de nombres de permiso a las entidades del catalogo.
     *
     * @param permissionNames nombres de permiso solicitados
     * @return las entidades encontradas
     * @throws ResourceNotFoundException si algun nombre no existe en el catalogo
     */
    private Set<Permission> resolvePermissions(Set<String> permissionNames) {
        Set<Permission> permissions = permissionRepository.findByNameIn(permissionNames);

        if (permissions.size() != permissionNames.size()) {
            Set<String> missing = new HashSet<>(permissionNames);
            permissions.forEach(permission -> missing.remove(permission.getName()));
            throw new ResourceNotFoundException("Permiso", "name", String.join(", ", missing));
        }

        return new HashSet<>(permissions);
    }

    /**
     * Recupera un rol o lanza la excepcion de recurso no encontrado.
     *
     * @param id identificador del rol
     * @return el rol encontrado
     * @throws ResourceNotFoundException si no existe
     */
    private Role getRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", id));
    }
}

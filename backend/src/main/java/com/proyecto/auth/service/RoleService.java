package com.proyecto.auth.service;

import com.proyecto.auth.dto.request.PermissionRequest;
import com.proyecto.auth.dto.request.RoleRequest;
import com.proyecto.auth.dto.response.PermissionResponse;
import com.proyecto.auth.dto.response.RoleResponse;

import java.util.List;

/**
 * Contrato de la gestion de roles y la consulta del catalogo de permisos.
 *
 * <p>Hay dos clases de permiso y conviene no confundirlas:</p>
 *
 * <ul>
 *   <li><b>Los que protegen el backend</b> ({@code USER_DELETE}, {@code FILE_UPLOAD}...)
 *       estan declarados en {@code PermissionName} porque hay anotaciones
 *       {@code @PreAuthorize} que los comprueban. Esos NO se pueden crear desde el panel:
 *       un nombre nuevo en la base de datos no aparece en ninguna comprobacion del
 *       servidor, asi que daria una falsa sensacion de proteccion.</li>
 *   <li><b>Los de solo interfaz</b> si se crean desde el panel. Sirven para decidir que
 *       muestra el frontend (una entrada del menu, una seccion) y funcionan sin tocar
 *       codigo porque la comprobacion del frontend es generica: compara por nombre
 *       cualquier permiso que venga en el token. Se guardan con
 *       {@code uiOnly = true} para que quede claro que no autorizan nada en el
 *       servidor.</li>
 * </ul>
 */
public interface RoleService {

    /**
     * Lista todos los roles con los permisos que concede cada uno.
     *
     * @return lista de roles
     */
    List<RoleResponse> findAllRoles();

    /**
     * Crea un rol nuevo con los permisos indicados.
     *
     * @param request nombre, descripcion y permisos del rol
     * @return el rol creado
     */
    RoleResponse createRole(RoleRequest request);

    /**
     * Actualiza la descripcion y los permisos de un rol existente.
     *
     * @param id      identificador del rol
     * @param request nuevos datos del rol
     * @return el rol actualizado
     */
    RoleResponse updateRole(Long id, RoleRequest request);

    /**
     * Elimina un rol.
     *
     * @param id identificador del rol a borrar
     */
    void deleteRole(Long id);

    /**
     * Lista el catalogo completo de permisos disponibles.
     *
     * @return lista de permisos
     */
    List<PermissionResponse> findAllPermissions();

    /**
     * Crea un permiso de solo interfaz.
     *
     * @param request nombre y descripcion del permiso
     * @return el permiso creado, siempre con {@code uiOnly = true}
     */
    PermissionResponse createUiPermission(PermissionRequest request);

    /**
     * Elimina un permiso de solo interfaz que no este en uso.
     *
     * @param id identificador del permiso
     */
    void deleteUiPermission(Long id);
}

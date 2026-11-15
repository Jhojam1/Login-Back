package com.proyecto.auth.controller;

import com.proyecto.auth.dto.request.PermissionRequest;
import com.proyecto.auth.dto.request.RoleRequest;
import com.proyecto.auth.dto.response.MessageResponse;
import com.proyecto.auth.dto.response.PermissionResponse;
import com.proyecto.auth.dto.response.RoleResponse;
import com.proyecto.auth.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de gestion de roles y consulta del catalogo de permisos.
 *
 * <p>Los roles se pueden crear, editar y borrar; los permisos son de solo lectura porque
 * solo conceden algo si hay codigo que los comprueba (ver {@code RoleService}).</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles y permisos", description = "Gestion de roles y catalogo de permisos")
public class RoleController {

    private final RoleService roleService;

    /**
     * Lista todos los roles con los permisos que concede cada uno.
     *
     * @return 200 con la lista de roles
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @Operation(summary = "Listar los roles disponibles")
    public ResponseEntity<List<RoleResponse>> findAllRoles() {
        return ResponseEntity.ok(roleService.findAllRoles());
    }

    /**
     * Crea un rol nuevo combinando permisos del catalogo.
     *
     * @param request nombre, descripcion y permisos del rol
     * @return 201 con el rol creado
     */
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Crear un rol (requiere permiso ROLE_WRITE)")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    /**
     * Actualiza la descripcion y los permisos de un rol.
     *
     * @param id      identificador del rol
     * @param request nuevos datos del rol
     * @return 200 con el rol actualizado
     */
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Modificar los permisos de un rol")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id,
                                                   @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    /**
     * Elimina un rol personalizado que no tenga usuarios asignados.
     *
     * @param id identificador del rol
     * @return 200 con un mensaje de confirmacion
     */
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Eliminar un rol personalizado")
    public ResponseEntity<MessageResponse> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(MessageResponse.of("Rol eliminado correctamente"));
    }

    /**
     * Lista el catalogo completo de permisos.
     *
     * @return 200 con la lista de permisos
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "Listar el catalogo de permisos")
    public ResponseEntity<List<PermissionResponse>> findAllPermissions() {
        return ResponseEntity.ok(roleService.findAllPermissions());
    }

    /**
     * Crea un permiso de solo interfaz.
     *
     * <p>Solo sirve para que el frontend decida que mostrar; no protege nada en el
     * servidor. Los permisos que si protegen operaciones se declaran en el codigo.</p>
     *
     * @param request nombre y descripcion del permiso
     * @return 201 con el permiso creado
     */
    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Crear un permiso de solo interfaz (requiere permiso PERMISSION_WRITE)")
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody PermissionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createUiPermission(request));
    }

    /**
     * Elimina un permiso de solo interfaz que no este en uso.
     *
     * @param id identificador del permiso
     * @return 200 con un mensaje de confirmacion
     */
    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Eliminar un permiso de solo interfaz")
    public ResponseEntity<MessageResponse> deletePermission(@PathVariable Long id) {
        roleService.deleteUiPermission(id);
        return ResponseEntity.ok(MessageResponse.of("Permiso eliminado correctamente"));
    }
}

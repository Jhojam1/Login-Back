package com.proyecto.auth.controller;

import com.proyecto.auth.dto.request.AssignRolesRequest;
import com.proyecto.auth.dto.request.CreateUserRequest;
import com.proyecto.auth.dto.request.UpdateUserRequest;
import com.proyecto.auth.dto.response.MessageResponse;
import com.proyecto.auth.dto.response.UserResponse;
import com.proyecto.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de administracion de usuarios.
 *
 * <p>Cada metodo declara arriba, con {@code @PreAuthorize}, el permiso que exige.
 * Tener la regla de seguridad pegada al metodo que protege la hace evidente al leer
 * el codigo, en lugar de esconderla en un archivo de configuracion lejano.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Administracion de usuarios, estados y roles")
public class UserController {

    private final UserService userService;

    /**
     * Crea un usuario desde el panel, eligiendo sus roles.
     *
     * @param request datos de la cuenta y roles a asignar
     * @return 201 con el usuario creado
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Crear un usuario (requiere permiso USER_CREATE)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    /**
     * Lista los usuarios de forma paginada.
     *
     * @param pageable parametros de paginacion recibidos por query string
     *                 ({@code ?page=0&size=20&sort=username,asc})
     * @return 200 con la pagina de usuarios
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Listar usuarios (requiere permiso USER_READ)")
    public ResponseEntity<Page<UserResponse>> findAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    /**
     * Devuelve el detalle de un usuario.
     *
     * @param id identificador del usuario
     * @return 200 con los datos del usuario
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Ver el detalle de un usuario")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * Actualiza los datos de un usuario.
     *
     * @param id      identificador del usuario
     * @param request campos a modificar
     * @return 200 con el usuario actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Actualizar los datos de un usuario")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    /**
     * Reemplaza los roles asignados a un usuario.
     *
     * @param id      identificador del usuario
     * @param request roles que quedaran asignados
     * @return 200 con el usuario y sus nuevos roles
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    @Operation(summary = "Asignar roles a un usuario (requiere permiso ROLE_ASSIGN)")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable Long id,
                                                    @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(userService.assignRoles(id, request));
    }

    /**
     * Activa o desactiva una cuenta.
     *
     * @param id      identificador del usuario
     * @param enabled {@code true} para activar, {@code false} para desactivar
     * @return 200 con el usuario actualizado
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Activar o desactivar una cuenta")
    public ResponseEntity<UserResponse> setEnabled(@PathVariable Long id,
                                                   @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setEnabled(id, enabled));
    }

    /**
     * Elimina un usuario.
     *
     * @param id identificador del usuario a borrar
     * @return 200 con un mensaje de confirmacion
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Eliminar un usuario (requiere permiso USER_DELETE)")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(MessageResponse.of("Usuario eliminado correctamente"));
    }
}

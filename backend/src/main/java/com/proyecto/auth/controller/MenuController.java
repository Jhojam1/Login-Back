package com.proyecto.auth.controller;

import com.proyecto.auth.dto.request.MenuItemRequest;
import com.proyecto.auth.dto.response.MenuItemResponse;
import com.proyecto.auth.dto.response.MessageResponse;
import com.proyecto.auth.security.UserPrincipal;
import com.proyecto.auth.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * Endpoints del menu de navegacion.
 *
 * <p>Son dos listados con proposito distinto: {@code GET /menu} devuelve el menu ya
 * filtrado para quien pregunta (lo consume el frontend en cada carga), y
 * {@code GET /menu/all} devuelve todas las entradas, incluidas las ocultas, para la
 * pantalla de gestion.</p>
 */
@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Menu", description = "Entradas de navegacion gestionables desde el panel")
public class MenuController {

    private final MenuService menuService;

    /**
     * Devuelve el menu que corresponde a la persona autenticada.
     *
     * <p>No exige ningun permiso especial: cualquiera que haya iniciado sesion necesita
     * su propio menu. El filtrado por permisos lo hace el servicio.</p>
     *
     * @param principal usuario autenticado, inyectado por Spring Security
     * @return 200 con las entradas que puede ver
     */
    @GetMapping
    @Operation(summary = "Obtener el menu del usuario autenticado")
    public ResponseEntity<List<MenuItemResponse>> myMenu(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(menuService.findVisibleFor(principal.getPermissionNames()));
    }

    /**
     * Devuelve todas las entradas para la pantalla de gestion.
     *
     * @return 200 con todas las entradas, incluidas las ocultas
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('MENU_WRITE')")
    @Operation(summary = "Listar todas las entradas del menu (requiere permiso MENU_WRITE)")
    public ResponseEntity<List<MenuItemResponse>> findAll() {
        return ResponseEntity.ok(menuService.findAll());
    }

    /**
     * Crea una entrada de menu.
     *
     * @param request datos de la entrada
     * @return 201 con la entrada creada
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MENU_WRITE')")
    @Operation(summary = "Crear una entrada del menu")
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(request));
    }

    /**
     * Actualiza una entrada del menu.
     *
     * @param id      identificador de la entrada
     * @param request nuevos datos
     * @return 200 con la entrada actualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MENU_WRITE')")
    @Operation(summary = "Modificar una entrada del menu")
    public ResponseEntity<MenuItemResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuService.update(id, request));
    }

    /**
     * Elimina una entrada del menu que no sea del sistema.
     *
     * @param id identificador de la entrada
     * @return 200 con un mensaje de confirmacion
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MENU_WRITE')")
    @Operation(summary = "Eliminar una entrada del menu")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ResponseEntity.ok(MessageResponse.of("Entrada eliminada correctamente"));
    }
}

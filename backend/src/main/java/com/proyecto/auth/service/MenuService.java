package com.proyecto.auth.service;

import com.proyecto.auth.dto.request.MenuItemRequest;
import com.proyecto.auth.dto.response.MenuItemResponse;

import java.util.List;
import java.util.Set;

/**
 * Contrato de la gestion del menu de navegacion.
 *
 * <p>El menu se guarda en base de datos para que un administrador pueda anadir, reordenar
 * u ocultar secciones sin que nadie recompile el frontend.</p>
 */
public interface MenuService {

    /**
     * Devuelve las entradas que debe ver una persona concreta.
     *
     * @param userPermissions permisos efectivos de esa persona
     * @return entradas visibles para ella, ya ordenadas
     */
    List<MenuItemResponse> findVisibleFor(Set<String> userPermissions);

    /**
     * Devuelve todas las entradas, incluidas las ocultas, para el panel de gestion.
     *
     * @return todas las entradas ordenadas
     */
    List<MenuItemResponse> findAll();

    /**
     * Crea una entrada nueva.
     *
     * @param request datos de la entrada
     * @return la entrada creada
     */
    MenuItemResponse create(MenuItemRequest request);

    /**
     * Actualiza una entrada existente.
     *
     * @param id      identificador de la entrada
     * @param request nuevos datos
     * @return la entrada actualizada
     */
    MenuItemResponse update(Long id, MenuItemRequest request);

    /**
     * Elimina una entrada que no sea del sistema.
     *
     * @param id identificador de la entrada
     */
    void delete(Long id);
}

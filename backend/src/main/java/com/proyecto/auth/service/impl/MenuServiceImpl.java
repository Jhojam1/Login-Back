package com.proyecto.auth.service.impl;

import com.proyecto.auth.domain.entity.MenuItem;
import com.proyecto.auth.dto.request.MenuItemRequest;
import com.proyecto.auth.dto.response.MenuItemResponse;
import com.proyecto.auth.exception.BusinessException;
import com.proyecto.auth.exception.DuplicateResourceException;
import com.proyecto.auth.exception.ResourceNotFoundException;
import com.proyecto.auth.mapper.UserMapper;
import com.proyecto.auth.repository.MenuItemRepository;
import com.proyecto.auth.repository.PermissionRepository;
import com.proyecto.auth.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Implementacion de la gestion del menu de navegacion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuItemRepository menuItemRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;

    /**
     * Devuelve las entradas visibles para los permisos indicados.
     *
     * <p>El filtrado se hace aqui, en el servidor, y no en el navegador: enviar al
     * frontend entradas que no debe ver le revelaria que secciones existen, aunque no
     * pudiera entrar en ellas.</p>
     *
     * @param userPermissions permisos efectivos de la persona
     * @return entradas que puede ver, ordenadas
     */
    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findVisibleFor(Set<String> userPermissions) {
        return menuItemRepository.findByVisibleTrueOrderByDisplayOrderAscIdAsc().stream()
                .filter(item -> isAllowed(item, userPermissions))
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Comprueba si una entrada es accesible con los permisos dados.
     *
     * @param item            entrada a evaluar
     * @param userPermissions permisos de la persona
     * @return {@code true} si no exige permiso o si la persona lo tiene
     */
    private boolean isAllowed(MenuItem item, Set<String> userPermissions) {
        if (!StringUtils.hasText(item.getRequiredPermission())) {
            return true;
        }
        return userPermissions.contains(item.getRequiredPermission());
    }

    /**
     * Devuelve todas las entradas para el panel de gestion.
     *
     * @return todas las entradas ordenadas
     */
    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAll() {
        return menuItemRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Crea una entrada nueva comprobando que la ruta no este repetida.
     *
     * @param request datos de la entrada
     * @return la entrada creada
     * @throws DuplicateResourceException si ya existe una entrada con esa ruta
     */
    @Override
    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        menuItemRepository.findByPath(request.path()).ifPresent(existing -> {
            throw new DuplicateResourceException("ruta del menu", request.path());
        });

        String permission = normalizePermission(request.requiredPermission());

        MenuItem item = MenuItem.builder()
                .label(request.label().trim())
                .path(request.path().trim())
                .icon(request.icon())
                .requiredPermission(permission)
                .displayOrder(request.displayOrder())
                .visible(request.visible() == null || request.visible())
                .systemItem(false)
                .build();

        MenuItem saved = menuItemRepository.save(item);
        log.info("Entrada de menu creada: {} -> {}", saved.getLabel(), saved.getPath());

        return userMapper.toResponse(saved);
    }

    /**
     * Actualiza una entrada existente.
     *
     * <p>Las entradas del sistema tambien se pueden editar: renombrarlas, reordenarlas,
     * ocultarlas o cambiar el permiso que exigen. Lo unico que no se les permite es
     * desaparecer.</p>
     *
     * @param id      identificador de la entrada
     * @param request nuevos datos
     * @return la entrada actualizada
     * @throws ResourceNotFoundException  si la entrada no existe
     * @throws DuplicateResourceException si la ruta nueva ya la usa otra entrada
     */
    @Override
    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem item = getItemOrThrow(id);

        String newPath = request.path().trim();
        menuItemRepository.findByPath(newPath).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new DuplicateResourceException("ruta del menu", newPath);
            }
        });

        item.setLabel(request.label().trim());
        item.setPath(newPath);
        item.setIcon(request.icon());
        item.setRequiredPermission(normalizePermission(request.requiredPermission()));
        item.setDisplayOrder(request.displayOrder());
        if (request.visible() != null) {
            item.setVisible(request.visible());
        }

        return userMapper.toResponse(menuItemRepository.save(item));
    }

    /**
     * Elimina una entrada que no sea del sistema.
     *
     * @param id identificador de la entrada
     * @throws BusinessException si se intenta borrar una entrada del sistema
     */
    @Override
    @Transactional
    public void delete(Long id) {
        MenuItem item = getItemOrThrow(id);

        if (item.isSystemItem()) {
            throw new BusinessException(HttpStatus.CONFLICT, "SYSTEM_MENU_ITEM",
                    "La entrada '%s' es del sistema. Puedes ocultarla, pero no eliminarla."
                            .formatted(item.getLabel()));
        }

        menuItemRepository.delete(item);
        log.info("Entrada de menu eliminada: {}", item.getLabel());
    }

    /**
     * Valida y normaliza el permiso exigido por una entrada.
     *
     * <p>Se comprueba que exista en el catalogo: una entrada que exigiera un permiso
     * inexistente seria invisible para todo el mundo, y averiguar por que llevaria un
     * buen rato.</p>
     *
     * @param rawPermission valor recibido del formulario, puede venir vacio
     * @return el nombre del permiso en mayusculas, o {@code null} si no exige ninguno
     * @throws ResourceNotFoundException si el permiso no existe en el catalogo
     */
    private String normalizePermission(String rawPermission) {
        if (!StringUtils.hasText(rawPermission)) {
            return null;
        }

        String name = rawPermission.trim().toUpperCase();
        if (!permissionRepository.existsByName(name)) {
            throw new ResourceNotFoundException("Permiso", "name", name);
        }

        return name;
    }

    /**
     * Recupera una entrada o lanza la excepcion de recurso no encontrado.
     *
     * @param id identificador de la entrada
     * @return la entrada encontrada
     * @throws ResourceNotFoundException si no existe
     */
    private MenuItem getItemOrThrow(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de menu", "id", id));
    }
}

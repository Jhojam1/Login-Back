package com.proyecto.auth.repository;

import com.proyecto.auth.domain.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de la entidad {@link MenuItem}.
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /**
     * Devuelve todas las entradas ordenadas tal como deben aparecer en el menu.
     *
     * @return entradas ordenadas por posicion y, a igualdad, por identificador
     */
    List<MenuItem> findAllByOrderByDisplayOrderAscIdAsc();

    /**
     * Devuelve solo las entradas visibles, ya ordenadas.
     *
     * @return entradas visibles ordenadas
     */
    List<MenuItem> findByVisibleTrueOrderByDisplayOrderAscIdAsc();

    /**
     * Busca una entrada por su ruta, para no duplicarlas.
     *
     * @param path ruta de la entrada
     * @return la entrada si existe
     */
    Optional<MenuItem> findByPath(String path);

    /**
     * Cuenta cuantas entradas exigen un permiso concreto.
     *
     * <p>Se consulta antes de borrar un permiso: si alguna entrada lo necesita, borrarlo
     * la volveria invisible para todo el mundo sin explicar por que.</p>
     *
     * @param requiredPermission nombre del permiso
     * @return numero de entradas que lo exigen
     */
    long countByRequiredPermission(String requiredPermission);
}

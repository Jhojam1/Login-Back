package com.proyecto.auth.repository;

import com.proyecto.auth.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

/**
 * Acceso a datos de la entidad {@link Permission}.
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Busca un permiso por su nombre.
     *
     * @param name nombre del permiso, por ejemplo {@code USER_READ}
     * @return el permiso si existe
     */
    Optional<Permission> findByName(String name);

    /**
     * Recupera de una sola consulta todos los permisos cuyos nombres se indiquen.
     *
     * @param names conjunto de nombres de permiso
     * @return permisos encontrados
     */
    Set<Permission> findByNameIn(Set<String> names);

    /**
     * Comprueba si ya existe un permiso con ese nombre.
     *
     * @param name nombre del permiso
     * @return {@code true} si ya existe
     */
    boolean existsByName(String name);
}

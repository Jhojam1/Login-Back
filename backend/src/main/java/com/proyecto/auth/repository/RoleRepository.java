package com.proyecto.auth.repository;

import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

/**
 * Acceso a datos de la entidad {@link Role}.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Busca un rol por su nombre textual.
     *
     * @param name nombre del rol, por ejemplo {@code ROLE_ADMIN}
     * @return el rol si existe
     */
    Optional<Role> findByName(String name);

    /**
     * Busca un rol usando el enum del catalogo, evitando literales sueltos en el codigo.
     *
     * @param roleName valor del catalogo de roles
     * @return el rol si existe
     */
    default Optional<Role> findByName(RoleName roleName) {
        return findByName(roleName.getAuthority());
    }

    /**
     * Recupera de una sola consulta todos los roles cuyos nombres se indiquen.
     *
     * @param names conjunto de nombres de rol
     * @return roles encontrados (puede devolver menos elementos que los solicitados)
     */
    Set<Role> findByNameIn(Set<String> names);

    /**
     * Comprueba si ya existe un rol con ese nombre.
     *
     * @param name nombre del rol
     * @return {@code true} si ya existe
     */
    boolean existsByName(String name);

    /**
     * Cuenta cuantos roles conceden un permiso concreto.
     *
     * <p>Se consulta antes de borrar un permiso de solo interfaz: si algun rol lo
     * concede, eliminarlo cambiaria en silencio lo que ven esas personas.</p>
     *
     * @param permissionId identificador del permiso
     * @return numero de roles que lo incluyen
     */
    long countByPermissionsId(Long permissionId);
}

package com.proyecto.auth.repository;

import com.proyecto.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acceso a datos de la entidad {@link User}.
 *
 * <p>Spring Data genera la implementacion en tiempo de ejecucion a partir de la firma
 * de los metodos, por lo que esta interfaz es la unica capa de persistencia que hay
 * que mantener.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su nombre de usuario exacto.
     *
     * @param username nombre de usuario
     * @return el usuario si existe
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca un usuario por su correo electronico exacto.
     *
     * @param email correo electronico
     * @return el usuario si existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario aceptando indistintamente su username o su email.
     *
     * <p>Permite que el formulario de login tenga un unico campo "usuario o correo",
     * que es lo que espera hoy cualquier persona que usa una aplicacion web.</p>
     *
     * @param identifier username o email introducido en el formulario
     * @return el usuario si alguno de los dos campos coincide
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    /**
     * Comprueba si un nombre de usuario ya esta ocupado.
     *
     * @param username nombre de usuario a verificar
     * @return {@code true} si ya existe
     */
    boolean existsByUsername(String username);

    /**
     * Comprueba si un correo electronico ya esta registrado.
     *
     * @param email correo a verificar
     * @return {@code true} si ya existe
     */
    boolean existsByEmail(String email);

    /**
     * Cuenta cuantos usuarios tienen asignado un rol concreto.
     *
     * <p>Se usa antes de borrar un rol: eliminarlo mientras alguien lo tiene le quitaria
     * sus permisos sin avisar, asi que es mejor rechazar la operacion y explicarlo.</p>
     *
     * @param roleId identificador del rol
     * @return numero de usuarios que lo tienen
     */
    long countByRolesId(Long roleId);
}

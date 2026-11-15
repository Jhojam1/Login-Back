package com.proyecto.auth.security;

import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adaptador entre la entidad de dominio {@link User} y el contrato {@code UserDetails}
 * que exige Spring Security.
 *
 * <p>Este patron adaptador mantiene la entidad JPA libre de anotaciones y metodos del
 * framework de seguridad: si manana se cambia de Spring Security a otra libreria, solo
 * hay que reescribir esta clase.</p>
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    /** Usuario de dominio que esta clase envuelve. */
    private final transient User user;

    /**
     * Construye la lista de autoridades que Spring Security evaluara.
     *
     * <p>Se incluyen DOS tipos de autoridad:</p>
     * <ul>
     *   <li>los roles con prefijo {@code ROLE_}, que habilitan {@code hasRole('ADMIN')};</li>
     *   <li>los permisos sin prefijo, que habilitan {@code hasAuthority('USER_READ')}.</li>
     * </ul>
     *
     * @return coleccion de roles y permisos del usuario
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Stream<String> roleNames = user.getRoles().stream()
                .map(Role::getName);

        Stream<String> permissionNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName());

        return Stream.concat(roleNames, permissionNames)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    /**
     * Devuelve solo los nombres de los roles, para incluirlos en el token y en las respuestas.
     *
     * @return conjunto de nombres de rol
     */
    public Set<String> getRoleNames() {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Devuelve solo los nombres de los permisos efectivos, sin duplicados aunque
     * varios roles concedan el mismo permiso.
     *
     * @return conjunto de nombres de permiso
     */
    public Set<String> getPermissionNames() {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());
    }

    /**
     * Devuelve el identificador del usuario.
     *
     * @return id del usuario en base de datos
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Devuelve la contrasena cifrada, que Spring Security compara con la introducida.
     *
     * @return hash BCrypt de la contrasena
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Devuelve el identificador con el que se autentica el usuario.
     *
     * @return nombre de usuario
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Indica si la cuenta sigue vigente. No se usa caducidad de cuentas, por lo que
     * siempre es verdadero.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta NO esta bloqueada por intentos fallidos.
     *
     * @return {@code true} si se puede iniciar sesion
     */
    @Override
    public boolean isAccountNonLocked() {
        return !user.isAccountLocked();
    }

    /**
     * Indica si las credenciales siguen vigentes. No se fuerza caducidad de contrasenas.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta esta habilitada por un administrador.
     *
     * @return {@code true} si la cuenta esta activa
     */
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}

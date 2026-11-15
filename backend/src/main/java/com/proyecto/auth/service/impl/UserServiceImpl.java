package com.proyecto.auth.service.impl;

import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.domain.enums.RoleName;
import com.proyecto.auth.dto.request.AssignRolesRequest;
import com.proyecto.auth.dto.request.CreateUserRequest;
import com.proyecto.auth.dto.request.UpdateUserRequest;
import com.proyecto.auth.dto.response.UserResponse;
import com.proyecto.auth.exception.DuplicateResourceException;
import com.proyecto.auth.exception.ResourceNotFoundException;
import com.proyecto.auth.mapper.UserMapper;
import com.proyecto.auth.repository.RoleRepository;
import com.proyecto.auth.repository.UserRepository;
import com.proyecto.auth.service.RefreshTokenService;
import com.proyecto.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementacion de la gestion de usuarios del panel de administracion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Crea un usuario desde el panel de administracion.
     *
     * <p>A diferencia del registro publico, aqui se pueden asignar los roles que se
     * quiera. Si no se indica ninguno, se otorga {@code ROLE_USER} para que la cuenta
     * nunca quede sin permisos y sin poder hacer nada.</p>
     *
     * @param request datos de la cuenta y roles a asignar
     * @return el usuario creado
     * @throws DuplicateResourceException si el username o el email ya existen
     * @throws ResourceNotFoundException  si alguno de los roles indicados no existe
     */
    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("username", request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("email", request.email());
        }

        Set<String> requestedRoles = CollectionUtils.isEmpty(request.roles())
                ? Set.of(RoleName.ROLE_USER.getAuthority())
                : request.roles();

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(true)
                .roles(resolveRoles(requestedRoles))
                .build();

        User saved = userRepository.save(user);
        log.info("Usuario {} creado desde el panel con roles {}", saved.getUsername(), requestedRoles);

        return userMapper.toResponse(saved);
    }

    /**
     * Devuelve los usuarios paginados.
     *
     * <p>Se pagina siempre, nunca se devuelve la tabla entera: con unos pocos miles de
     * usuarios un {@code findAll()} sin limite tumbaria la memoria del servidor.</p>
     *
     * @param pageable pagina y ordenacion solicitadas
     * @return pagina de usuarios ya convertidos a DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    /**
     * Busca un usuario por su identificador.
     *
     * @param id identificador del usuario
     * @return el usuario encontrado
     * @throws ResourceNotFoundException si no existe
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getUserOrThrow(id));
    }

    /**
     * Devuelve los datos del usuario autenticado en la peticion actual.
     *
     * @param username nombre de usuario extraido del token
     * @return sus datos publicos
     * @throws ResourceNotFoundException si el usuario ya no existe
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));
        return userMapper.toResponse(user);
    }

    /**
     * Actualiza unicamente los campos que llegan informados.
     *
     * @param id      identificador del usuario a modificar
     * @param request campos a cambiar; los nulos se ignoran
     * @return el usuario ya actualizado
     * @throws DuplicateResourceException si el nuevo correo pertenece a otra cuenta
     */
    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getUserOrThrow(id);

        if (StringUtils.hasText(request.firstName())) {
            user.setFirstName(request.firstName());
        }
        if (StringUtils.hasText(request.lastName())) {
            user.setLastName(request.lastName());
        }
        if (StringUtils.hasText(request.email()) && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("email", request.email());
            }
            user.setEmail(request.email());
        }
        if (request.enabled() != null) {
            applyEnabledState(user, request.enabled());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Reemplaza por completo los roles de un usuario.
     *
     * <p>Tras el cambio se revocan sus tokens de refresco: el token de acceso que tiene
     * en el navegador sigue llevando los roles antiguos, asi que obligarle a renovarlo
     * es la forma de que los permisos nuevos (o la perdida de permisos) surtan efecto.</p>
     *
     * @param id      identificador del usuario
     * @param request roles que quedaran asignados
     * @return el usuario con sus nuevos roles
     * @throws ResourceNotFoundException si alguno de los roles indicados no existe
     */
    @Override
    @Transactional
    public UserResponse assignRoles(Long id, AssignRolesRequest request) {
        User user = getUserOrThrow(id);

        user.setRoles(resolveRoles(request.roles()));
        User saved = userRepository.save(user);

        refreshTokenService.revokeAll(saved);
        log.info("Roles del usuario {} actualizados a {}", saved.getUsername(), request.roles());

        return userMapper.toResponse(saved);
    }

    /**
     * Activa o desactiva una cuenta.
     *
     * @param id      identificador del usuario
     * @param enabled nuevo estado
     * @return el usuario actualizado
     */
    @Override
    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = getUserOrThrow(id);
        applyEnabledState(user, enabled);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Elimina un usuario del sistema junto con sus sesiones.
     *
     * @param id identificador del usuario a borrar
     * @throws ResourceNotFoundException si no existe
     */
    @Override
    @Transactional
    public void delete(Long id) {
        User user = getUserOrThrow(id);
        refreshTokenService.revokeAll(user);
        userRepository.delete(user);
        log.info("Usuario {} eliminado", user.getUsername());
    }

    /**
     * Aplica el cambio de estado y, si se desactiva la cuenta, cierra sus sesiones.
     *
     * @param user    usuario afectado
     * @param enabled nuevo estado
     */
    private void applyEnabledState(User user, boolean enabled) {
        user.setEnabled(enabled);
        if (!enabled) {
            // Sin esto, alguien desactivado seguiria navegando hasta que su token caducase.
            refreshTokenService.revokeAll(user);
        }
    }

    /**
     * Traduce un conjunto de nombres de rol a las entidades correspondientes.
     *
     * <p>Si alguno no existe, falla indicando exactamente cual: devolver silenciosamente
     * menos roles de los pedidos dejaria al usuario con menos permisos de los que el
     * administrador cree haberle dado, y eso es muy dificil de depurar despues.</p>
     *
     * @param roleNames nombres de rol solicitados
     * @return las entidades de rol encontradas
     * @throws ResourceNotFoundException si algun nombre no corresponde a un rol existente
     */
    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = roleRepository.findByNameIn(roleNames);

        if (roles.size() != roleNames.size()) {
            Set<String> missing = new HashSet<>(roleNames);
            roles.forEach(role -> missing.remove(role.getName()));
            throw new ResourceNotFoundException("Rol", "name", String.join(", ", missing));
        }

        return new HashSet<>(roles);
    }

    /**
     * Recupera un usuario o lanza la excepcion de recurso no encontrado.
     *
     * <p>Centralizar esta busqueda evita repetir el mismo {@code orElseThrow} en
     * cada metodo publico de la clase.</p>
     *
     * @param id identificador del usuario
     * @return el usuario encontrado
     * @throws ResourceNotFoundException si no existe
     */
    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }
}

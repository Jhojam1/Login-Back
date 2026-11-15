package com.proyecto.auth.service;

import com.proyecto.auth.dto.request.AssignRolesRequest;
import com.proyecto.auth.dto.request.CreateUserRequest;
import com.proyecto.auth.dto.request.UpdateUserRequest;
import com.proyecto.auth.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de la gestion de usuarios desde el panel de administracion.
 */
public interface UserService {

    /**
     * Crea un usuario desde el panel de administracion, con los roles indicados.
     *
     * @param request datos de la cuenta y roles a asignar
     * @return el usuario creado
     */
    UserResponse create(CreateUserRequest request);

    /**
     * Devuelve los usuarios paginados.
     *
     * @param pageable pagina y ordenacion solicitadas
     * @return pagina de usuarios
     */
    Page<UserResponse> findAll(Pageable pageable);

    /**
     * Busca un usuario por su identificador.
     *
     * @param id identificador del usuario
     * @return el usuario encontrado
     */
    UserResponse findById(Long id);

    /**
     * Devuelve los datos del usuario que esta usando la aplicacion ahora mismo.
     *
     * @param username nombre de usuario extraido del token
     * @return sus datos publicos
     */
    UserResponse findByUsername(String username);

    /**
     * Actualiza los campos no nulos de un usuario.
     *
     * @param id      identificador del usuario a modificar
     * @param request campos a cambiar
     * @return el usuario ya actualizado
     */
    UserResponse update(Long id, UpdateUserRequest request);

    /**
     * Reemplaza por completo el conjunto de roles de un usuario.
     *
     * @param id      identificador del usuario
     * @param request roles que quedaran asignados
     * @return el usuario con sus nuevos roles
     */
    UserResponse assignRoles(Long id, AssignRolesRequest request);

    /**
     * Activa o desactiva una cuenta.
     *
     * <p>Al desactivarla se revocan sus tokens de refresco para que la expulsion
     * sea inmediata y no haya que esperar a que caduque el token de acceso.</p>
     *
     * @param id      identificador del usuario
     * @param enabled nuevo estado
     * @return el usuario actualizado
     */
    UserResponse setEnabled(Long id, boolean enabled);

    /**
     * Elimina un usuario del sistema.
     *
     * @param id identificador del usuario a borrar
     */
    void delete(Long id);
}

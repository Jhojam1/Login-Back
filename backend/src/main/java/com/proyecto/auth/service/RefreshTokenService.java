package com.proyecto.auth.service;

import com.proyecto.auth.domain.entity.RefreshToken;
import com.proyecto.auth.domain.entity.User;

/**
 * Contrato del ciclo de vida de los tokens de refresco.
 *
 * <p>Separarlo del servicio de autenticacion mantiene cada clase con una sola razon
 * para cambiar: aqui solo se decide como se crean, validan y revocan estos tokens.</p>
 */
public interface RefreshTokenService {

    /**
     * Crea y persiste un token de refresco nuevo para el usuario.
     *
     * @param user      propietario del token
     * @param ipAddress IP desde la que se solicita
     * @param userAgent cliente desde el que se solicita
     * @return el token recien creado
     */
    RefreshToken create(User user, String ipAddress, String userAgent);

    /**
     * Recupera un token verificando que exista, no este revocado y no haya expirado.
     *
     * @param token valor recibido del cliente
     * @return el token valido
     * @throws com.proyecto.auth.exception.InvalidTokenException si no es utilizable
     */
    RefreshToken verify(String token);

    /**
     * Revoca un token concreto (cierre de sesion en un dispositivo).
     *
     * @param token valor del token a invalidar
     */
    void revoke(String token);

    /**
     * Revoca todos los tokens activos de un usuario.
     *
     * @param user usuario afectado
     */
    void revokeAll(User user);

    /**
     * Borra de la base de datos los tokens caducados o revocados.
     *
     * @return numero de registros eliminados
     */
    int purgeExpired();
}

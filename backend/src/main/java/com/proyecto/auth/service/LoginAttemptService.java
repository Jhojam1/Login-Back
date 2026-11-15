package com.proyecto.auth.service;

import com.proyecto.auth.domain.entity.User;

/**
 * Contrato de la politica de bloqueo por intentos fallidos.
 *
 * <p>Es la defensa contra ataques de fuerza bruta: tras N fallos consecutivos la
 * cuenta se bloquea temporalmente, lo que hace inviable probar miles de contrasenas.</p>
 */
public interface LoginAttemptService {

    /**
     * Registra un intento fallido y bloquea la cuenta si se supera el limite.
     *
     * @param identifier usuario o correo con el que se intento entrar
     */
    void registerFailure(String identifier);

    /**
     * Limpia el contador de fallos tras un acceso correcto.
     *
     * @param user usuario que acaba de autenticarse
     */
    void registerSuccess(User user);

    /**
     * Comprueba si el bloqueo de una cuenta ya cumplio su tiempo y, en tal caso,
     * la desbloquea.
     *
     * @param user usuario a revisar
     * @return {@code true} si la cuenta quedo desbloqueada en esta llamada
     */
    boolean unlockIfTimeExpired(User user);
}

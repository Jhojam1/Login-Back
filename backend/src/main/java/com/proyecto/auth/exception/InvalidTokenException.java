package com.proyecto.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un token (de acceso o de refresco) es invalido, ha expirado
 * o fue revocado. Se traduce en un HTTP 401.
 */
public class InvalidTokenException extends BusinessException {

    /**
     * Crea la excepcion con el motivo concreto del rechazo.
     *
     * @param message explicacion del problema con el token
     */
    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", message);
    }
}

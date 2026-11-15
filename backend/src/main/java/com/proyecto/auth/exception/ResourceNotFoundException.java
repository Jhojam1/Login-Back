package com.proyecto.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando se solicita un recurso que no existe (usuario, rol o permiso).
 * Se traduce en un HTTP 404.
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Crea la excepcion indicando que recurso no se encontro.
     *
     * @param resource nombre del recurso, por ejemplo {@code Usuario}
     * @param field    campo por el que se busco, por ejemplo {@code id}
     * @param value    valor buscado
     */
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "%s no encontrado con %s = '%s'".formatted(resource, field, value));
    }
}

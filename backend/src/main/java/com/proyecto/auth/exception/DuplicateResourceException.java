package com.proyecto.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza al intentar crear un recurso cuyo valor unico ya existe
 * (por ejemplo un username o un email repetido). Se traduce en un HTTP 409.
 */
public class DuplicateResourceException extends BusinessException {

    /**
     * Crea la excepcion indicando que valor esta duplicado.
     *
     * @param field campo unico afectado, por ejemplo {@code email}
     * @param value valor que ya estaba registrado
     */
    public DuplicateResourceException(String field, Object value) {
        super(HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                "Ya existe un registro con %s = '%s'".formatted(field, value));
    }
}

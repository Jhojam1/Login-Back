package com.proyecto.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepcion base de todos los errores de negocio de la aplicacion.
 *
 * <p>Lleva consigo el codigo HTTP y un codigo interno estable, de modo que el manejador
 * global no necesita una cadena de {@code instanceof} para decidir que responder:
 * basta con leer los datos que trae la propia excepcion (polimorfismo en lugar de
 * condicionales, principio abierto/cerrado).</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** Codigo HTTP con el que debe responderse al cliente. */
    private final HttpStatus status;

    /** Codigo interno estable, pensado para que el frontend reaccione sin parsear textos. */
    private final String code;

    /**
     * Crea una excepcion de negocio.
     *
     * @param status  codigo HTTP de la respuesta
     * @param code    codigo interno, por ejemplo {@code USER_NOT_FOUND}
     * @param message mensaje legible para la persona usuaria
     */
    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

package com.proyecto.auth.exception;

import com.proyecto.auth.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Manejador centralizado de errores de toda la API REST.
 *
 * <p>Gracias a {@code @RestControllerAdvice} ningun controlador necesita bloques
 * try/catch: los controladores se limitan a orquestar y aqui se decide como se ve
 * cada error. Esto cumple el principio de responsabilidad unica y garantiza que el
 * frontend reciba siempre la misma estructura de error.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Traduce cualquier error de negocio propio usando el estado y codigo que trae dentro.
     *
     * @param ex      excepcion capturada
     * @param request peticion que la provoco, para registrar la ruta
     * @return respuesta con el codigo HTTP declarado en la excepcion
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("Error de negocio [{}] en {}: {}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request, null);
    }

    /**
     * Traduce los fallos de validacion de los DTOs en un 400 con el detalle campo a campo.
     *
     * @param ex      excepcion lanzada por {@code @Valid}
     * @param request peticion que la provoco
     * @return respuesta 400 con la lista de campos invalidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Valor invalido" : error.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Revisa los datos enviados", request, details);
    }

    /**
     * Traduce credenciales incorrectas en un 401 con un mensaje deliberadamente generico.
     *
     * <p>No se distingue entre "usuario inexistente" y "contrasena incorrecta" para no
     * ayudar a quien intenta averiguar que cuentas existen (enumeracion de usuarios).</p>
     *
     * @param ex      excepcion de Spring Security
     * @param request peticion que la provoco
     * @return respuesta 401
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                                  HttpServletRequest request) {
        log.warn("Intento de acceso fallido en {}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                "Usuario o contrasena incorrectos", request, null);
    }

    /**
     * Responde 423 (Locked) cuando la cuenta esta bloqueada por intentos fallidos.
     *
     * @param ex      excepcion de Spring Security
     * @param request peticion que la provoco
     * @return respuesta 423
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLocked(LockedException ex, HttpServletRequest request) {
        log.warn("Intento de acceso a una cuenta bloqueada desde {}", request.getRemoteAddr());
        return build(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Tu cuenta esta bloqueada temporalmente por demasiados intentos fallidos. "
                        + "Vuelve a intentarlo en unos minutos.", request, null);
    }

    /**
     * Responde 403 cuando la cuenta existe pero esta deshabilitada.
     *
     * @param ex      excepcion de Spring Security
     * @param request peticion que la provoco
     * @return respuesta 403
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                "Tu cuenta esta desactivada. Contacta con un administrador.", request, null);
    }

    /**
     * Responde 403 cuando el usuario esta autenticado pero le faltan permisos o roles.
     *
     * @param ex      excepcion lanzada por {@code @PreAuthorize}
     * @param request peticion que la provoco
     * @return respuesta 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                                HttpServletRequest request) {
        log.warn("Acceso denegado a {}", request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "No tienes permisos para realizar esta accion", request, null);
    }

    /**
     * Responde 404 cuando la ruta solicitada no existe.
     *
     * <p>Sin este manejador, la red de seguridad de mas abajo capturaria la excepcion de
     * Spring y devolveria un 500, haciendo creer que el servidor se rompio cuando en
     * realidad la URL estaba mal escrita.</p>
     *
     * @param ex      excepcion lanzada por Spring al no encontrar la ruta
     * @param request peticion que la provoco
     * @return respuesta 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND",
                "La ruta solicitada no existe", request, null);
    }

    /**
     * Responde 405 cuando la ruta existe pero no admite ese metodo HTTP.
     *
     * @param ex      excepcion lanzada por Spring
     * @param request peticion que la provoco
     * @return respuesta 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "El metodo %s no esta permitido en esta ruta".formatted(ex.getMethod()),
                request, null);
    }

    /**
     * Responde 400 cuando el cuerpo de la peticion no es un JSON valido.
     *
     * @param ex      excepcion lanzada al deserializar
     * @param request peticion que la provoco
     * @return respuesta 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_BODY",
                "El cuerpo de la peticion no es un JSON valido", request, null);
    }

    /**
     * Responde 400 cuando un parametro de la URL no tiene el tipo esperado,
     * por ejemplo {@code /users/abc} donde se espera un numero.
     *
     * @param ex      excepcion lanzada al convertir el parametro
     * @param request peticion que la provoco
     * @return respuesta 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "El valor de '%s' no tiene el formato esperado".formatted(ex.getName()),
                request, null);
    }

    /**
     * Red de seguridad para cualquier error no previsto.
     *
     * <p>Registra la traza completa en el log del servidor pero devuelve al cliente un
     * mensaje neutro, para no exponer detalles internos de la aplicacion.</p>
     *
     * @param ex      excepcion no contemplada
     * @param request peticion que la provoco
     * @return respuesta 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ha ocurrido un error inesperado. Intentalo de nuevo mas tarde.", request, null);
    }

    /**
     * Construye el cuerpo de error comun a todas las respuestas.
     *
     * @param status  codigo HTTP
     * @param code    codigo interno
     * @param message mensaje legible
     * @param request peticion original, de donde se toma la ruta
     * @param details detalle opcional de validacion
     * @return la respuesta HTTP completamente formada
     */
    private ResponseEntity<ApiErrorResponse> build(HttpStatus status,
                                                   String code,
                                                   String message,
                                                   HttpServletRequest request,
                                                   List<Map<String, String>> details) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}

package com.proyecto.auth.service;

import com.proyecto.auth.dto.request.ChangePasswordRequest;
import com.proyecto.auth.dto.request.LoginRequest;
import com.proyecto.auth.dto.request.RefreshTokenRequest;
import com.proyecto.auth.dto.request.RegisterRequest;
import com.proyecto.auth.dto.response.AuthResponse;
import com.proyecto.auth.dto.response.IntrospectTokenResponse;
import com.proyecto.auth.dto.response.SessionStatusResponse;
import com.proyecto.auth.dto.response.UserResponse;

/**
 * Contrato del servicio de autenticacion.
 *
 * <p>Se define como interfaz para que los controladores dependan de una abstraccion y
 * no de una implementacion concreta (principio de inversion de dependencias). Eso
 * ademas permite sustituirla por un doble de prueba en los tests sin levantar la
 * base de datos.</p>
 */
public interface AuthService {

    /**
     * Da de alta un usuario nuevo con el rol basico del sistema.
     *
     * @param request datos del formulario de registro
     * @return el usuario creado, ya sin la contrasena
     */
    UserResponse register(RegisterRequest request);

    /**
     * Valida las credenciales y emite el par de tokens de la sesion.
     *
     * @param request   credenciales introducidas
     * @param ipAddress IP de origen, para auditoria
     * @param userAgent cliente de origen, para auditoria
     * @return tokens de acceso y refresco junto con los datos del usuario
     */
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    /**
     * Canjea un token de refresco valido por un token de acceso nuevo.
     *
     * <p>Aplica rotacion: el token de refresco usado se revoca y se entrega uno nuevo,
     * de modo que si alguien roba un token antiguo este ya no sirve.</p>
     *
     * @param request   token de refresco a canjear
     * @param ipAddress IP de origen, para auditoria
     * @param userAgent cliente de origen, para auditoria
     * @return un par de tokens nuevo
     */
    AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent);

    /**
     * Cierra la sesion revocando el token de refresco indicado.
     *
     * @param refreshToken token a invalidar
     */
    void logout(String refreshToken);

    /**
     * Cierra todas las sesiones abiertas del usuario en todos sus dispositivos.
     *
     * @param username usuario cuyas sesiones se cierran
     */
    void logoutFromAllDevices(String username);

    /**
     * Verifica un token de acceso en nombre de otro microservicio.
     *
     * @param token token de acceso a validar
     * @return estado del token y, si es valido, la identidad y permisos de su portador
     */
    IntrospectTokenResponse introspect(String token);

    /**
     * Cambia la contrasena del usuario autenticado y cierra sus demas sesiones.
     *
     * @param username usuario que solicita el cambio
     * @param request  contrasena actual y nueva
     */
    void changePassword(String username, ChangePasswordRequest request);

    /**
     * Indica si un usuario tiene la sesion iniciada, sin necesidad de su token.
     *
     * <p>Pensado para microservicios que solo quieren mostrar informacion, por ejemplo
     * marcar quien esta conectado en un listado.</p>
     *
     * <p><b>No sirve para autorizar.</b> Que alguien tenga una sesion abierta no
     * demuestra que sea quien hace la peticion. Para eso esta {@link #introspect}.</p>
     *
     * @param identifier nombre de usuario o correo
     * @return estado de la sesion de esa persona
     */
    SessionStatusResponse checkSessionStatus(String identifier);
}

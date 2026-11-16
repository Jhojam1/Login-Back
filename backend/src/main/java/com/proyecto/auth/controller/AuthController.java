package com.proyecto.auth.controller;

import com.proyecto.auth.config.SecurityProperties;
import com.proyecto.auth.dto.request.ChangePasswordRequest;
import com.proyecto.auth.dto.request.IntrospectTokenRequest;
import com.proyecto.auth.dto.request.LoginRequest;
import com.proyecto.auth.dto.request.RefreshTokenRequest;
import com.proyecto.auth.dto.request.RegisterRequest;
import com.proyecto.auth.dto.request.SessionStatusRequest;
import com.proyecto.auth.dto.response.AuthResponse;
import com.proyecto.auth.dto.response.IntrospectTokenResponse;
import com.proyecto.auth.dto.response.MessageResponse;
import com.proyecto.auth.dto.response.SessionStatusResponse;
import com.proyecto.auth.dto.response.UserResponse;
import com.proyecto.auth.service.AuthService;
import com.proyecto.auth.service.UserService;
import com.proyecto.auth.util.RequestMetadataExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.auth.security.UserPrincipal;

/**
 * Endpoints publicos de autenticacion: registro, login, refresco, logout
 * e introspeccion de tokens para otros microservicios.
 *
 * <p>El controlador no contiene logica de negocio: valida la entrada (mediante
 * {@code @Valid}), delega en el servicio y traduce el resultado a una respuesta HTTP.
 * Esa es toda su responsabilidad.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Registro, inicio de sesion y gestion de tokens")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RequestMetadataExtractor metadataExtractor;
    private final SecurityProperties securityProperties;

    /**
     * Crea una cuenta nueva con el rol basico del sistema.
     *
     * @param request datos del formulario de registro
     * @return 201 con los datos publicos del usuario creado
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar un usuario nuevo")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Inicia sesion y devuelve el par de tokens.
     *
     * @param request     credenciales introducidas
     * @param httpRequest peticion HTTP, de la que se extraen IP y navegador para auditoria
     * @return 200 con el token de acceso, el de refresco y los datos del usuario
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion y obtener los tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(
                request,
                metadataExtractor.extractIpAddress(httpRequest),
                metadataExtractor.extractUserAgent(httpRequest));

        return ResponseEntity.ok(response);
    }

    /**
     * Canjea un token de refresco por un par de tokens nuevo.
     *
     * @param request     token de refresco a canjear
     * @param httpRequest peticion HTTP, para la auditoria del token nuevo
     * @return 200 con los tokens renovados
     */
    @PostMapping("/refresh")
    @Operation(summary = "Renovar el token de acceso sin volver a introducir la contrasena")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                HttpServletRequest httpRequest) {
        AuthResponse response = authService.refresh(
                request,
                metadataExtractor.extractIpAddress(httpRequest),
                metadataExtractor.extractUserAgent(httpRequest));

        return ResponseEntity.ok(response);
    }

    /**
     * Cierra la sesion actual revocando el token de refresco.
     *
     * @param request token de refresco del dispositivo que cierra sesion
     * @return 200 con un mensaje de confirmacion
     */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar la sesion actual")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(MessageResponse.of("Sesion cerrada correctamente"));
    }

    /**
     * Cierra la sesion del usuario en todos sus dispositivos.
     *
     * @param principal usuario autenticado, inyectado por Spring Security
     * @return 200 con un mensaje de confirmacion
     */
    @PostMapping("/logout-all")
    @Operation(summary = "Cerrar la sesion en todos los dispositivos")
    public ResponseEntity<MessageResponse> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logoutFromAllDevices(principal.getUsername());
        return ResponseEntity.ok(MessageResponse.of("Se cerraron todas las sesiones"));
    }

    /**
     * Devuelve los datos del usuario autenticado, incluidos sus roles y permisos.
     *
     * <p>Es el endpoint que llama el frontend al cargar la aplicacion para saber
     * que menus debe mostrar.</p>
     *
     * @param principal usuario autenticado, inyectado por Spring Security
     * @return 200 con los datos del usuario
     */
    @GetMapping("/me")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.findByUsername(principal.getUsername()));
    }

    /**
     * Cambia la contrasena del usuario autenticado.
     *
     * @param principal usuario autenticado
     * @param request   contrasena actual y nueva
     * @return 200 con un mensaje de confirmacion
     */
    @PostMapping("/change-password")
    @Operation(summary = "Cambiar la propia contrasena (cierra todas las sesiones)")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUsername(), request);
        return ResponseEntity.ok(MessageResponse.of(
                "Contrasena actualizada. Vuelve a iniciar sesion."));
    }

    /**
     * Valida un token de acceso por cuenta de otro microservicio.
     *
     * <p><b>Este es el endpoint que consume el equipo del servicio de archivos (S3).</b>
     * Recibe un token y responde si esta activo junto con los roles y permisos actuales
     * de su portador.</p>
     *
     * <p>Se protege con una clave compartida en la cabecera {@code X-Service-Key} en
     * lugar de con un JWT, porque quien llama es una maquina y no una persona. La clave
     * se configura por variable de entorno en cada EC2.</p>
     *
     * @param request    token a validar
     * @param serviceKey clave compartida del servicio que pregunta
     * @return 200 con el estado del token, o 401 si la clave de servicio es incorrecta
     */
    @PostMapping("/introspect")
    @Operation(summary = "Validar un token desde otro microservicio (maquina a maquina)")
    public ResponseEntity<IntrospectTokenResponse> introspect(
            @Valid @RequestBody IntrospectTokenRequest request,
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {

        if (!isServiceKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(IntrospectTokenResponse.inactive("INVALID_SERVICE_KEY"));
        }

        return ResponseEntity.ok(authService.introspect(request.token()));
    }

    /**
     * Indica si un usuario tiene la sesion iniciada, a partir de su nombre.
     *
     * <p>Pensado para servicios que solo quieren mostrar informacion (por ejemplo
     * marcar quien esta conectado) y que no reciben el token de la persona.</p>
     *
     * <p><b>No usar para autorizar.</b> Cualquiera puede escribir un nombre de usuario
     * en el cuerpo de la peticion, asi que la respuesta no demuestra quien esta
     * llamando. Para decidir si se permite una operacion hay que usar
     * {@code /introspect} con el token de quien la solicita.</p>
     *
     * @param request    usuario a consultar
     * @param serviceKey clave compartida del servicio que pregunta
     * @return 200 con el estado de la sesion, o 401 si la clave de servicio es incorrecta
     */
    @PostMapping("/session-status")
    @Operation(summary = "Saber si un usuario tiene sesion iniciada (solo informativo)")
    public ResponseEntity<SessionStatusResponse> sessionStatus(
            @Valid @RequestBody SessionStatusRequest request,
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {

        if (!isServiceKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(authService.checkSessionStatus(request.username()));
    }

    /**
     * Comprueba la clave compartida que identifica a un microservicio aliado.
     *
     * <p>Si no se configuro ninguna clave (entorno de desarrollo) se acepta cualquier
     * llamada; en produccion la variable debe estar siempre definida.</p>
     *
     * @param serviceKey valor recibido en la cabecera
     * @return {@code true} si la llamada esta autorizada
     */
    private boolean isServiceKeyValid(String serviceKey) {
        String expected = securityProperties.getServiceApiKey();
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return expected.equals(serviceKey);
    }
}

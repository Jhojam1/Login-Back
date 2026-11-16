package com.proyecto.auth.service.impl;

import com.proyecto.auth.config.JwtProperties;
import com.proyecto.auth.config.MfaProperties;
import com.proyecto.auth.domain.entity.RefreshToken;
import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.domain.entity.VerificationCode;
import com.proyecto.auth.domain.enums.RoleName;
import com.proyecto.auth.dto.request.ChangePasswordRequest;
import com.proyecto.auth.dto.request.LoginRequest;
import com.proyecto.auth.dto.request.RefreshTokenRequest;
import com.proyecto.auth.dto.request.RegisterRequest;
import com.proyecto.auth.dto.request.ResendCodeRequest;
import com.proyecto.auth.dto.request.VerifyCodeRequest;
import com.proyecto.auth.dto.response.AuthResponse;
import com.proyecto.auth.dto.response.IntrospectTokenResponse;
import com.proyecto.auth.dto.response.SessionStatusResponse;
import com.proyecto.auth.dto.response.UserResponse;
import com.proyecto.auth.exception.BusinessException;
import com.proyecto.auth.exception.DuplicateResourceException;
import com.proyecto.auth.exception.ResourceNotFoundException;
import com.proyecto.auth.mapper.UserMapper;
import com.proyecto.auth.repository.RoleRepository;
import com.proyecto.auth.repository.UserRepository;
import com.proyecto.auth.security.JwtTokenProvider;
import com.proyecto.auth.security.UserPrincipal;
import com.proyecto.auth.service.AuthService;
import com.proyecto.auth.service.LoginAttemptService;
import com.proyecto.auth.service.RefreshTokenService;
import com.proyecto.auth.service.VerificationCodeService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion de los casos de uso de autenticacion.
 *
 * <p>Esta clase orquesta: valida reglas de negocio y delega las tareas especializadas
 * en colaboradores (cifrado, emision de JWT, tokens de refresco, control de intentos).
 * No contiene ni consultas SQL ni manipulacion directa de JWT, lo que la mantiene corta
 * y facil de leer.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final VerificationCodeService verificationCodeService;
    private final MfaProperties mfaProperties;
    private final UserMapper userMapper;

    /**
     * Registra un usuario nuevo.
     *
     * <p>Comprueba que el username y el email esten libres, cifra la contrasena con
     * BCrypt y asigna el rol basico {@code ROLE_USER}. Nunca concede roles elevados
     * desde el registro publico: eso solo puede hacerlo un administrador.</p>
     *
     * @param request datos del formulario de registro
     * @return el usuario creado
     * @throws DuplicateResourceException si el username o el email ya existen
     */
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("username", request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("email", request.email());
        }

        Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "name", RoleName.ROLE_USER));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(true)
                .build();
        user.addRole(defaultRole);

        User saved = userRepository.save(user);
        log.info("Nuevo usuario registrado: {}", saved.getUsername());

        return userMapper.toResponse(saved);
    }

    /**
     * Autentica a un usuario y emite su par de tokens.
     *
     * @param request   credenciales introducidas
     * @param ipAddress IP de origen, para auditoria
     * @param userAgent cliente de origen, para auditoria
     * @return tokens y datos del usuario
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Antes de validar la contrasena se revisa si el bloqueo temporal ya vencio,
        // para no rechazar a alguien que ya cumplio su penalizacion.
        userRepository.findByUsernameOrEmail(request.identifier())
                .ifPresent(loginAttemptService::unlockIfTimeExpired);

        UserPrincipal principal = authenticate(request);
        loginAttemptService.registerSuccess(principal.getUser());

        // Con la verificacion en dos pasos apagada, la contrasena basta y se entregan
        // los tokens aqui mismo.
        if (!mfaProperties.isEnabled()) {
            return buildAuthResponse(principal, ipAddress, userAgent);
        }

        // Con la verificacion encendida NO se entrega ningun token todavia: solo el
        // identificador del intento. Si los tokens viajaran ahora, bastaria con
        // ignorar la pantalla del codigo para entrar igualmente.
        VerificationCode verificacion = verificationCodeService
                .createAndSend(principal.getUser(), ipAddress, userAgent);

        log.info("Login de {} pendiente de verificacion por correo", principal.getUsername());

        return buildPendingResponse(principal.getUser(), verificacion);
    }

    /**
     * Completa el inicio de sesion comprobando el codigo recibido por correo.
     *
     * @param request   identificador del intento y codigo introducido
     * @param ipAddress IP de origen
     * @param userAgent cliente de origen
     * @return los tokens
     */
    @Override
    @Transactional
    public AuthResponse verifyCode(VerifyCodeRequest request, String ipAddress, String userAgent) {
        User user = verificationCodeService.verify(request.challengeId(), request.code());

        // Se vuelve a comprobar el estado de la cuenta: pudieron desactivarla en los
        // minutos que la persona tardo en mirar el correo.
        if (!user.isEnabled()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                    "Tu cuenta esta desactivada");
        }

        log.info("Verificacion superada por {}", user.getUsername());

        return buildAuthResponse(new UserPrincipal(user), ipAddress, userAgent);
    }

    /**
     * Vuelve a enviar el codigo de un intento en curso.
     *
     * @param request   identificador del intento
     * @param ipAddress IP de origen
     * @param userAgent cliente de origen
     * @return la respuesta con el intento nuevo, sin tokens
     */
    @Override
    @Transactional
    public AuthResponse resendCode(ResendCodeRequest request, String ipAddress, String userAgent) {
        VerificationCode verificacion = verificationCodeService
                .resend(request.challengeId(), ipAddress, userAgent);

        return buildPendingResponse(verificacion.getUser(), verificacion);
    }

    /**
     * Construye la respuesta de "falta verificar", sin ningun token dentro.
     *
     * @param user         usuario que esta intentando entrar
     * @param verificacion codigo recien generado
     * @return respuesta con el identificador del intento
     */
    private AuthResponse buildPendingResponse(User user, VerificationCode verificacion) {
        return AuthResponse.builder()
                .mfaRequired(true)
                .challengeId(verificacion.getChallengeId())
                .maskedEmail(maskEmail(user.getEmail()))
                .codeExpiresInSeconds(mfaProperties.getExpirationMinutes() * 60)
                .build();
    }

    /**
     * Tapa parte del correo para poder decir a donde se envio el codigo sin
     * revelar la direccion completa a quien solo acerto la contrasena.
     *
     * @param email correo completo
     * @return el correo parcialmente oculto, por ejemplo {@code jh***@correo.com}
     */
    private String maskEmail(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 2) {
            return "***" + email.substring(Math.max(arroba, 0));
        }
        return email.substring(0, 2) + "***" + email.substring(arroba);
    }

    /**
     * Delega la validacion de credenciales en Spring Security y contabiliza los fallos.
     *
     * @param request credenciales introducidas
     * @return el usuario autenticado
     * @throws BadCredentialsException si el usuario o la contrasena no coinciden
     */
    private UserPrincipal authenticate(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));
            return (UserPrincipal) authentication.getPrincipal();
        } catch (BadCredentialsException ex) {
            loginAttemptService.registerFailure(request.identifier());
            throw ex;
        }
    }

    /**
     * Renueva la sesion aplicando rotacion de tokens de refresco.
     *
     * <p>El token usado se revoca y se entrega uno nuevo. Si alguien intentara reutilizar
     * el antiguo, la verificacion fallaria, lo que delata el robo del token.</p>
     *
     * @param request   token de refresco a canjear
     * @param ipAddress IP de origen, para auditoria
     * @param userAgent cliente de origen, para auditoria
     * @return un par de tokens nuevo
     */
    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        RefreshToken stored = refreshTokenService.verify(request.refreshToken());
        User user = stored.getUser();

        if (!user.isEnabled()) {
            refreshTokenService.revokeAll(user);
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                    "Tu cuenta esta desactivada");
        }

        refreshTokenService.revoke(stored.getToken());

        return buildAuthResponse(new UserPrincipal(user), ipAddress, userAgent);
    }

    /**
     * Cierra la sesion actual revocando el token de refresco recibido.
     *
     * @param refreshToken token a invalidar
     */
    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    /**
     * Cierra todas las sesiones del usuario en todos sus dispositivos.
     *
     * @param username usuario cuyas sesiones se cierran
     */
    @Override
    @Transactional
    public void logoutFromAllDevices(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));
        refreshTokenService.revokeAll(user);
    }

    /**
     * Valida un token de acceso en nombre de otro microservicio.
     *
     * <p>Ademas de comprobar la firma y la expiracion, vuelve a consultar el usuario en
     * la base de datos. Asi, si un administrador le quito un rol o desactivo la cuenta
     * hace un minuto, el servicio aliado se entera al instante en lugar de esperar a
     * que caduque el token.</p>
     *
     * @param token token de acceso a validar
     * @return estado del token y, si procede, identidad y permisos actuales
     */
    @Override
    @Transactional(readOnly = true)
    public IntrospectTokenResponse introspect(String token) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(token);
        } catch (ExpiredJwtException ex) {
            return IntrospectTokenResponse.inactive("TOKEN_EXPIRED");
        } catch (JwtException | IllegalArgumentException ex) {
            return IntrospectTokenResponse.inactive("TOKEN_INVALID");
        }

        return userRepository.findByUsername(claims.getSubject())
                .map(user -> describeToken(user, claims))
                .orElseGet(() -> IntrospectTokenResponse.inactive("USER_NOT_FOUND"));
    }

    /**
     * Construye la respuesta de introspeccion comprobando el estado actual del usuario.
     *
     * @param user   usuario recuperado de la base de datos
     * @param claims claims del token ya verificado
     * @return respuesta activa con sus permisos, o inactiva con el motivo del rechazo
     */
    private IntrospectTokenResponse describeToken(User user, Claims claims) {
        if (!user.isEnabled()) {
            return IntrospectTokenResponse.inactive("USER_DISABLED");
        }
        if (user.isAccountLocked()) {
            return IntrospectTokenResponse.inactive("USER_LOCKED");
        }

        return IntrospectTokenResponse.builder()
                .active(true)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(userMapper.extractRoleNames(user))
                .permissions(userMapper.extractPermissionNames(user))
                .issuedAt(claims.getIssuedAt().toInstant())
                .expiresAt(claims.getExpiration().toInstant())
                .issuer(claims.getIssuer())
                .build();
    }

    /**
     * Cambia la contrasena del usuario y cierra todas sus sesiones abiertas.
     *
     * @param username usuario que solicita el cambio
     * @param request  contrasena actual y nueva
     * @throws BusinessException si la contrasena actual no coincide o la nueva es igual
     */
    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD",
                    "La contrasena actual no es correcta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAME_PASSWORD",
                    "La nueva contrasena debe ser distinta de la actual");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Un cambio de contrasena invalida todas las sesiones: es la forma de expulsar
        // a quien pudiera haber robado la cuenta.
        refreshTokenService.revokeAll(user);
        log.info("El usuario {} cambio su contrasena", username);
    }

    /**
     * Comprueba si un usuario tiene alguna sesion abierta.
     *
     * <p>Se acepta tanto el nombre de usuario como el correo, igual que en el login,
     * para que quien pregunte no tenga que saber cual de los dos guardamos.</p>
     *
     * <p>Una cuenta desactivada o bloqueada se informa como no conectada aunque le
     * queden tokens sin caducar: no va a poder usar el sistema, asi que decir que
     * esta conectada seria enganoso.</p>
     *
     * @param identifier nombre de usuario o correo
     * @return estado de la sesion
     */
    @Override
    @Transactional(readOnly = true)
    public SessionStatusResponse checkSessionStatus(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier)
                .map(user -> {
                    boolean utilizable = user.isEnabled() && !user.isAccountLocked();
                    long sesiones = utilizable ? refreshTokenService.countActiveSessions(user) : 0;

                    return SessionStatusResponse.builder()
                            .username(user.getUsername())
                            .exists(true)
                            .loggedIn(sesiones > 0)
                            .activeSessions(sesiones)
                            .lastLoginAt(user.getLastLoginAt())
                            .build();
                })
                .orElseGet(() -> SessionStatusResponse.notFound(identifier));
    }

    /**
     * Genera el par de tokens y arma la respuesta de autenticacion.
     *
     * @param principal usuario autenticado
     * @param ipAddress IP de origen, para auditoria
     * @param userAgent cliente de origen, para auditoria
     * @return respuesta lista para devolver al cliente
     */
    private AuthResponse buildAuthResponse(UserPrincipal principal, String ipAddress, String userAgent) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        RefreshToken refreshToken = refreshTokenService.create(principal.getUser(), ipAddress, userAgent);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpirationSeconds())
                .mfaRequired(false)
                .user(userMapper.toResponse(principal.getUser()))
                .build();
    }
}

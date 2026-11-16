package com.proyecto.auth.service.impl;

import com.proyecto.auth.config.JwtProperties;
import com.proyecto.auth.domain.entity.RefreshToken;
import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.exception.InvalidTokenException;
import com.proyecto.auth.repository.RefreshTokenRepository;
import com.proyecto.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Implementacion del ciclo de vida de los tokens de refresco.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    /** Numero de bytes aleatorios del token; 64 bytes son 512 bits de entropia. */
    private static final int TOKEN_BYTES = 64;

    /** Generador criptograficamente seguro: Math.random() NO sirve para esto. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * Crea y persiste un token de refresco opaco y aleatorio.
     *
     * <p>A diferencia del token de acceso, este no es un JWT ni contiene informacion:
     * es solo una referencia a una fila de la base de datos, lo que permite revocarlo.</p>
     *
     * @param user      propietario del token
     * @param ipAddress IP desde la que se solicita
     * @param userAgent cliente desde el que se solicita
     * @return el token recien creado
     */
    @Override
    @Transactional
    public RefreshToken create(User user, String ipAddress, String userAgent) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken token = RefreshToken.builder()
                .token(value)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(truncate(userAgent))
                .build();

        return refreshTokenRepository.save(token);
    }

    /**
     * Recupera un token comprobando que exista y siga siendo utilizable.
     *
     * @param token valor recibido del cliente
     * @return el token valido
     * @throws InvalidTokenException si no existe, esta revocado o ha expirado
     */
    @Override
    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("El token de refresco no existe"));

        if (stored.isRevoked()) {
            throw new InvalidTokenException("El token de refresco fue revocado. Inicia sesion de nuevo.");
        }
        if (stored.isExpired()) {
            throw new InvalidTokenException("El token de refresco ha caducado. Inicia sesion de nuevo.");
        }
        return stored;
    }

    /**
     * Marca un token como revocado. Es idempotente: revocar dos veces no falla,
     * algo importante porque el frontend puede reintentar el cierre de sesion.
     *
     * @param token valor del token a invalidar
     */
    @Override
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(stored -> {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
        });
    }

    /**
     * Revoca de una sola consulta todos los tokens activos del usuario.
     *
     * @param user usuario afectado
     */
    @Override
    @Transactional
    public void revokeAll(User user) {
        int revoked = refreshTokenRepository.revokeAllByUser(user);
        log.info("Se revocaron {} sesiones del usuario {}", revoked, user.getUsername());
    }

    /**
     * Elimina los tokens caducados o revocados para que la tabla no crezca sin control.
     *
     * @return numero de registros eliminados
     */
    @Override
    @Transactional
    public int purgeExpired() {
        return refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
    }

    /**
     * Cuenta las sesiones abiertas de un usuario consultando sus tokens de refresco.
     *
     * @param user usuario a consultar
     * @return numero de sesiones abiertas
     */
    @Override
    @Transactional(readOnly = true)
    public long countActiveSessions(User user) {
        return refreshTokenRepository.countActiveSessions(user, Instant.now());
    }

    /**
     * Recorta el user-agent al tamano de la columna para evitar errores de insercion.
     *
     * @param userAgent cabecera recibida, puede ser nula
     * @return la cadena recortada a 255 caracteres, o {@code null}
     */
    private String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }
}

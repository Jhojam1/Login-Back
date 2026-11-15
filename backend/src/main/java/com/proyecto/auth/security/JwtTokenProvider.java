package com.proyecto.auth.security;

import com.proyecto.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Responsable unico de crear, firmar y leer tokens JWT.
 *
 * <p>Ninguna otra clase manipula la libreria JJWT directamente: si manana se cambia de
 * HMAC a claves RSA, o de JJWT a otra libreria, el cambio se limita a este archivo.</p>
 *
 * <p>Estructura del token emitido (payload):</p>
 * <pre>
 * sub          -> nombre de usuario
 * uid          -> id numerico del usuario
 * email        -> correo del usuario
 * roles        -> lista de roles, por ejemplo [ROLE_ADMIN]
 * permissions  -> lista de permisos, por ejemplo [USER_READ, FILE_UPLOAD]
 * iss          -> emisor (auth-service)
 * iat / exp    -> emitido en / expira en
 * jti          -> identificador unico de este token
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /** Claim que guarda el identificador numerico del usuario. */
    public static final String CLAIM_USER_ID = "uid";
    /** Claim que guarda el correo del usuario. */
    public static final String CLAIM_EMAIL = "email";
    /** Claim que guarda la lista de roles. */
    public static final String CLAIM_ROLES = "roles";
    /** Claim que guarda la lista de permisos. */
    public static final String CLAIM_PERMISSIONS = "permissions";

    private final JwtProperties jwtProperties;

    /**
     * Construye la clave de firma a partir del secreto configurado.
     *
     * @return clave HMAC-SHA lista para firmar o verificar
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token de acceso firmado para el usuario indicado.
     *
     * <p>Los roles y permisos viajan DENTRO del token: asi los servicios aliados pueden
     * autorizar sin consultar la base de datos en cada peticion.</p>
     *
     * @param principal usuario autenticado
     * @return cadena JWT compacta lista para enviar al cliente
     */
    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpirationMs());

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_USER_ID, principal.getId())
                .claim(CLAIM_EMAIL, principal.getUser().getEmail())
                .claim(CLAIM_ROLES, List.copyOf(principal.getRoleNames()))
                .claim(CLAIM_PERMISSIONS, List.copyOf(principal.getPermissionNames()))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Lee y valida un token, devolviendo sus claims.
     *
     * @param token cadena JWT recibida del cliente
     * @return los claims del token
     * @throws JwtException si la firma no coincide, el token expiro o esta mal formado
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Comprueba si un token es valido sin lanzar excepciones.
     *
     * @param token cadena JWT a verificar
     * @return {@code true} si el token es autentico y esta vigente
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token expirado: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token invalido: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extrae el nombre de usuario (claim sub) de un token valido.
     *
     * @param token cadena JWT
     * @return nombre de usuario contenido en el token
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Intenta leer los claims devolviendo un Optional vacio si el token no sirve.
     *
     * @param token cadena JWT
     * @return los claims, o vacio si el token es invalido o expiro
     */
    public Optional<Claims> tryParseClaims(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Extrae un claim de tipo lista como conjunto de cadenas.
     *
     * @param claims    claims ya validados
     * @param claimName nombre del claim a leer (roles o permissions)
     * @return conjunto de valores; vacio si el claim no existe
     */
    public Set<String> extractStringSet(Claims claims, String claimName) {
        Object raw = claims.get(claimName);
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }
}

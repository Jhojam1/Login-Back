package com.proyecto.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Token de refresco persistido en base de datos.
 *
 * <p>El token de acceso (JWT) es de vida corta y no se guarda: se valida con la firma.
 * El token de refresco, en cambio, vive horas o dias, por lo que se almacena para poder
 * <b>revocarlo</b> (cerrar sesion, robo de token, cambio de contrasena). Esto resuelve la
 * principal limitacion de JWT: que por si solo no se puede invalidar antes de expirar.</p>
 */
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_token", columnList = "token", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = {"id", "expiresAt", "revoked"})
public class RefreshToken extends Auditable {

    /** Identificador tecnico generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Valor opaco y aleatorio del token (no es un JWT, no contiene informacion). */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /** Usuario propietario del token. Se carga de forma diferida: no siempre se necesita. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Momento exacto en el que el token deja de ser valido. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Marca de revocacion manual (logout, rotacion o compromiso de seguridad). */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /** Direccion IP desde la que se emitio el token; util para auditoria. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Navegador o cliente que solicito el token; util para auditoria. */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Indica si el token ya caduco comparando su fecha de expiracion con el instante actual.
     *
     * @return {@code true} si la fecha de expiracion ya paso
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Indica si el token puede usarse para obtener un nuevo token de acceso.
     *
     * @return {@code true} solo si no esta revocado y no ha expirado
     */
    public boolean isUsable() {
        return !revoked && !isExpired();
    }
}

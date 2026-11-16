package com.proyecto.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Codigo de un solo uso enviado por correo para completar el inicio de sesion.
 *
 * <p>Cuando la verificacion en dos pasos esta activada, acertar la contrasena ya no
 * basta: el backend genera un codigo, lo manda al correo de la persona y no entrega
 * los tokens hasta que lo escribe. Asi, quien robe una contrasena sigue sin poder
 * entrar si no tiene tambien acceso al buzon.</p>
 *
 * <p>Esta tabla es temporal por naturaleza: una tarea programada borra los codigos
 * caducados para que no se acumulen.</p>
 */
@Entity
@Table(
        name = "verification_codes",
        indexes = {
                @Index(name = "idx_verification_challenge", columnList = "challenge_id", unique = true),
                @Index(name = "idx_verification_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = {"id", "challengeId", "expiresAt", "consumed"})
public class VerificationCode extends Auditable {

    /** Identificador tecnico generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador publico de este intento de inicio de sesion.
     *
     * <p>Es lo unico que se le devuelve al navegador tras acertar la contrasena. Se usa
     * en lugar del nombre de usuario a proposito: si el segundo paso aceptara un
     * username, cualquiera podria probar codigos contra cuentas ajenas. Con este valor
     * aleatorio solo puede continuar quien acaba de superar el primer paso.</p>
     */
    @Column(name = "challenge_id", nullable = false, unique = true, length = 64)
    private String challengeId;

    /** Usuario que esta intentando entrar. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Resumen (hash) del codigo, nunca el codigo en claro.
     *
     * <p>Si alguien consiguiera leer la base de datos, no podria usar los codigos que
     * estan en vuelo. Es la misma razon por la que las contrasenas se guardan cifradas.</p>
     */
    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    /** Momento en que el codigo deja de servir. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Se marca al usarlo, para que un mismo codigo no valga dos veces. */
    @Column(name = "consumed", nullable = false)
    @Builder.Default
    private boolean consumed = false;

    /**
     * Intentos fallidos sobre este codigo.
     *
     * <p>Un codigo de seis cifras tiene un millon de combinaciones: sin un limite de
     * intentos, un programa las prueba todas en minutos.</p>
     */
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    /** IP desde la que se pidio el codigo; util para auditoria. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Cliente desde el que se pidio; util para auditoria. */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Indica si el codigo ya caduco.
     *
     * @return {@code true} si la fecha de expiracion ya paso
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Indica si el codigo todavia se puede verificar.
     *
     * @param maxAttempts numero maximo de intentos permitidos
     * @return {@code true} si no se ha usado, no ha caducado y quedan intentos
     */
    public boolean isUsable(int maxAttempts) {
        return !consumed && !isExpired() && attempts < maxAttempts;
    }
}

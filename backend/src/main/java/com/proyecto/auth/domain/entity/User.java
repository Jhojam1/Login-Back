package com.proyecto.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Usuario del sistema.
 *
 * <p>Esta clase es una entidad de dominio pura: no implementa {@code UserDetails} de
 * Spring Security. La adaptacion a ese contrato se hace en
 * {@code com.proyecto.auth.security.UserPrincipal}, de modo que el modelo de datos no
 * queda acoplado al framework de seguridad (inversion de dependencias, la "D" de SOLID).</p>
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username", unique = true),
                @Index(name = "idx_user_email", columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = {"id", "username", "email", "enabled"})
public class User extends Auditable {

    /** Identificador tecnico generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario unico con el que se inicia sesion. */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Correo electronico unico; sirve como via alternativa de inicio de sesion. */
    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    /**
     * Contrasena cifrada con BCrypt.
     *
     * <p>Nunca se guarda en texto plano y nunca se expone en un DTO de respuesta.</p>
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /** Nombre de la persona. */
    @Column(name = "first_name", length = 80)
    private String firstName;

    /** Apellido de la persona. */
    @Column(name = "last_name", length = 80)
    private String lastName;

    /** Indica si la cuenta esta activa; si es {@code false} el login se rechaza. */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** Indica si la cuenta esta bloqueada por exceso de intentos fallidos. */
    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private boolean accountLocked = false;

    /** Contador de intentos de login fallidos consecutivos. */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    /** Momento en que se bloqueo la cuenta; se usa para calcular el desbloqueo automatico. */
    @Column(name = "lock_time")
    private Instant lockTime;

    /** Ultimo inicio de sesion exitoso (dato informativo para el panel). */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Roles asignados al usuario.
     *
     * <p>{@code EAGER} por el mismo motivo que en {@code Role}: los roles y sus permisos
     * se necesitan en cada autenticacion para generar el JWT.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Asigna un rol al usuario.
     *
     * @param role rol a asignar; se ignora si es {@code null}
     */
    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    /**
     * Quita un rol al usuario.
     *
     * @param role rol a revocar
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /**
     * Devuelve el nombre completo de la persona para mostrarlo en la interfaz.
     *
     * @return nombre y apellido concatenados, o el {@code username} si no hay datos
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return username;
        }
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    /**
     * Reinicia el contador de intentos fallidos, desbloquea la cuenta y guarda
     * la fecha del acceso. Se invoca tras un inicio de sesion exitoso.
     */
    public void registerSuccessfulLogin() {
        this.failedAttempts = 0;
        this.accountLocked = false;
        this.lockTime = null;
        this.lastLoginAt = Instant.now();
    }
}

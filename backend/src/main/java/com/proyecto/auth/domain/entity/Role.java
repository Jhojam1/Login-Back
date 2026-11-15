package com.proyecto.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Rol: agrupacion de permisos que se asigna a los usuarios.
 *
 * <p>Separar rol y permiso permite cambiar lo que puede hacer un grupo completo de
 * personas modificando un unico registro, sin tocar el codigo ni a cada usuario.</p>
 */
@Entity
@Table(name = "roles", indexes = @Index(name = "idx_role_name", columnList = "name", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = {"id", "name"})
public class Role extends Auditable {

    /** Identificador tecnico generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre unico del rol; coincide con un valor de {@code RoleName} (incluye el prefijo ROLE_). */
    @Column(name = "name", nullable = false, unique = true, length = 60)
    private String name;

    /** Descripcion legible del rol. */
    @Column(name = "description", length = 180)
    private String description;

    /**
     * Permisos concedidos por este rol.
     *
     * <p>Se carga con {@code EAGER} porque siempre que se lee un rol durante la
     * autenticacion se necesitan sus permisos para construir el token; asi se evita el
     * problema N+1 y las {@code LazyInitializationException} fuera de la transaccion.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Anade un permiso al rol.
     *
     * @param permission permiso a conceder; se ignora si es {@code null}
     */
    public void addPermission(Permission permission) {
        if (permission != null) {
            this.permissions.add(permission);
        }
    }

    /**
     * Quita un permiso del rol.
     *
     * @param permission permiso a revocar
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }
}

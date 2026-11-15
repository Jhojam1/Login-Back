package com.proyecto.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Permiso: unidad minima de autorizacion ("que puede hacer alguien").
 *
 * <p>Ejemplos: {@code USER_READ}, {@code FILE_UPLOAD}. Los permisos nunca se asignan
 * directamente al usuario, sino a traves de roles, lo que mantiene el modelo RBAC
 * (Role Based Access Control) simple y auditable.</p>
 */
@Entity
@Table(name = "permissions", indexes = @Index(name = "idx_permission_name", columnList = "name", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = {"id", "name"})
public class Permission extends Auditable {

    /** Identificador tecnico generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre unico del permiso; coincide con un valor de {@code PermissionName}. */
    @Column(name = "name", nullable = false, unique = true, length = 60)
    private String name;

    /** Texto legible que explica para que sirve el permiso (se muestra en el panel admin). */
    @Column(name = "description", length = 180)
    private String description;

    /**
     * Indica si el permiso solo sirve para decidir que muestra la interfaz.
     *
     * <p>Cuando es {@code false} (los del enum {@code PermissionName}), hay codigo en el
     * backend que lo comprueba con {@code @PreAuthorize} y por tanto protege una
     * operacion real.</p>
     *
     * <p>Cuando es {@code true}, el permiso se creo desde el panel y vive solo en la base
     * de datos. El frontend lo usa para mostrar u ocultar menus y secciones, lo cual
     * funciona sin tocar codigo porque la comprobacion del frontend es generica: compara
     * por nombre cualquier permiso que llegue en el token. Lo que NO hace es proteger
     * nada en el servidor, y la distincion se guarda aqui para que quede explicita.</p>
     */
    @Column(name = "ui_only", nullable = false)
    @Builder.Default
    private boolean uiOnly = false;
}

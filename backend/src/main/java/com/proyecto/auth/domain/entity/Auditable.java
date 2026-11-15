package com.proyecto.auth.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Clase base con los campos de auditoría comunes a todas las entidades.
 *
 * <p>Se marca como {@code @MappedSuperclass} para que Hibernate copie estas columnas
 * en cada tabla hija sin crear una tabla propia. Centralizar la auditoría aquí evita
 * repetir los mismos dos campos en cada entidad (principio DRY) y respeta el
 * principio de responsabilidad única: esta clase solo se encarga del "cuándo".</p>
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    /** Fecha y hora en que el registro fue insertado. La asigna Spring Data automáticamente. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha y hora de la última modificación del registro. */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}

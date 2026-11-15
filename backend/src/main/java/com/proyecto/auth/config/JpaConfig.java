package com.proyecto.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita dos capacidades transversales de la aplicacion:
 *
 * <ul>
 *   <li><b>Auditoria JPA</b>: rellena automaticamente {@code createdAt} y
 *       {@code updatedAt} en todas las entidades que heredan de {@code Auditable}.</li>
 *   <li><b>Tareas programadas</b>: permite ejecutar la limpieza periodica de tokens
 *       caducados.</li>
 * </ul>
 */
@Configuration
@EnableJpaAuditing
@EnableScheduling
public class JpaConfig {
}

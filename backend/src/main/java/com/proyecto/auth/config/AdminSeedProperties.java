package com.proyecto.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del administrador inicial que se crea la primera vez que arranca la aplicacion.
 *
 * <p>La contrasena se lee de la configuracion (y en produccion de una variable de
 * entorno), nunca esta escrita en el codigo fuente.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.admin")
public class AdminSeedProperties {

    /** Si es {@code false}, no se crea ningun usuario administrador al arrancar. */
    private boolean enabled = true;

    /** Nombre de usuario del administrador inicial. */
    private String username = "admin";

    /** Correo del administrador inicial. */
    private String email = "admin@proyecto.com";

    /** Contrasena del administrador inicial; cambiala en el primer acceso. */
    private String password = "Admin.2026";
}

package com.proyecto.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de la documentacion interactiva (Swagger UI).
 *
 * <p>Una vez levantada la aplicacion, la documentacion esta en
 * {@code http://localhost:8080/swagger-ui.html}. Sirve para que el equipo del servicio
 * de archivos pruebe el endpoint de introspeccion sin escribir una linea de codigo.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Nombre del esquema de seguridad referenciado desde los controladores. */
    private static final String BEARER_SCHEME = "bearerAuth";

    /**
     * Define los metadatos de la API y el esquema de autenticacion por token.
     *
     * @return la definicion OpenAPI que consume Swagger UI
     */
    @Bean
    public OpenAPI authServiceOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .name(BEARER_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Pega aqui el accessToken devuelto por /api/v1/auth/login");

        return new OpenAPI()
                .info(new Info()
                        .title("Servicio de Autenticacion")
                        .version("1.0.0")
                        .description("API de login con JWT, roles y permisos. "
                                + "Incluye el endpoint de introspeccion que usan los demas microservicios.")
                        .contact(new Contact().name("Equipo de Autenticacion")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme));
    }
}

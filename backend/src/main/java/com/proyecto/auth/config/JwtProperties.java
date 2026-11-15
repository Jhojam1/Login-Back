package com.proyecto.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parametros de configuracion del JWT, leidos del bloque {@code app.jwt} de
 * {@code application.yml} (y por tanto sobreescribibles con variables de entorno
 * en el servidor EC2, sin recompilar).
 *
 * <p>Tipar la configuracion en una clase en lugar de esparcir {@code @Value} por todo
 * el codigo permite validarla al arrancar: si el secreto falta o es demasiado corto,
 * la aplicacion no levanta en vez de fallar en la primera peticion.</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Clave secreta para firmar los tokens (algoritmo HMAC-SHA256).
     *
     * <p>Debe tener al menos 32 caracteres y NUNCA debe subirse al repositorio:
     * en produccion se inyecta como variable de entorno {@code APP_JWT_SECRET}.</p>
     */
    @NotBlank
    private String secret;

    /** Nombre del emisor que se escribe en el claim {@code iss} del token. */
    @NotBlank
    private String issuer = "auth-service";

    /** Vida del token de acceso en milisegundos (por defecto 15 minutos). */
    @Min(60_000)
    private long accessTokenExpirationMs = 900_000L;

    /** Vida del token de refresco en milisegundos (por defecto 7 dias). */
    @Min(300_000)
    private long refreshTokenExpirationMs = 604_800_000L;

    /**
     * Devuelve la vida del token de acceso expresada en segundos,
     * que es la unidad que espera el campo {@code expiresIn} de la respuesta HTTP.
     *
     * @return segundos de validez del token de acceso
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}

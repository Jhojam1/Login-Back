package com.proyecto.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Parametros de seguridad ajustables sin tocar codigo, leidos del bloque {@code app.security}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** Origenes permitidos por CORS (la URL del frontend en S3, CloudFront o EC2). */
    private List<String> allowedOrigins = List.of("http://localhost:5173");

    /** Numero de intentos fallidos consecutivos antes de bloquear la cuenta. */
    private int maxFailedAttempts = 5;

    /** Minutos que permanece bloqueada la cuenta antes de desbloquearse sola. */
    private long lockDurationMinutes = 15;

    /**
     * Clave compartida que deben enviar los microservicios aliados (por ejemplo el de S3)
     * en la cabecera {@code X-Service-Key} para poder usar el endpoint de introspeccion.
     *
     * <p>Si se deja vacia, la introspeccion queda abierta a cualquiera que alcance la red,
     * lo cual solo es aceptable en desarrollo.</p>
     */
    private String serviceApiKey = "";

    /** Numero de rondas de BCrypt; a mas rondas, mas lento de romper y de calcular. */
    private int bcryptStrength = 12;
}

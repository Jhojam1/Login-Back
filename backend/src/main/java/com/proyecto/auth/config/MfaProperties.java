package com.proyecto.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de la verificacion en dos pasos por correo, bajo {@code app.mfa}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mfa")
public class MfaProperties {

    /**
     * Si esta activada, el login no entrega los tokens hasta que la persona escribe
     * el codigo que recibe por correo.
     *
     * <p>Activada por defecto. Conviene saber que, si la funcion Lambda dejara de
     * responder, nadie podria iniciar sesion: para desactivarla temporalmente basta
     * con poner {@code app.mfa.enabled: false} en la configuracion.</p>
     */
    private boolean enabled = true;

    /** Numero de cifras del codigo. */
    private int codeLength = 6;

    /** Minutos que el codigo sigue siendo valido. */
    private long expirationMinutes = 10;

    /** Intentos fallidos permitidos antes de invalidar el codigo. */
    private int maxAttempts = 5;

    /**
     * Segundos que hay que esperar entre un envio y el siguiente.
     *
     * <p>Sin esta espera, pulsar "reenviar" muchas veces generaria una avalancha de
     * correos y podria agotar la cuota de SES.</p>
     */
    private long resendCooldownSeconds = 60;
}

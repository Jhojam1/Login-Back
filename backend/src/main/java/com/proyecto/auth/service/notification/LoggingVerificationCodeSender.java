package com.proyecto.auth.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementacion de desarrollo: en lugar de enviar un correo, escribe el codigo
 * en el log del servidor.
 *
 * <p>Se activa cuando {@code app.lambda.enabled} es falso, que es lo habitual en local.
 * Permite recorrer el flujo completo de verificacion sin credenciales de AWS.</p>
 *
 * <p>Obviamente no debe usarse en produccion: cualquiera con acceso a los registros
 * veria los codigos. Por eso el aviso que imprime es tan visible.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.lambda.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingVerificationCodeSender implements VerificationCodeSender {

    /**
     * Escribe el codigo en el log en lugar de enviarlo.
     *
     * @param email correo de la persona
     * @param code  codigo generado
     */
    @Override
    public void send(String email, String code) {
        log.warn("=================================================================");
        log.warn(" MODO DESARROLLO: el correo no se envia de verdad");
        log.warn(" Destinatario: {}", email);
        log.warn(" Codigo:       {}", code);
        log.warn(" Activa app.lambda.enabled=true para enviarlo con AWS Lambda");
        log.warn("=================================================================");
    }
}

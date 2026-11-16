package com.proyecto.auth.config;

import com.proyecto.auth.service.RefreshTokenService;
import com.proyecto.auth.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que limpia los tokens de refresco inservibles.
 *
 * <p>Cada sesion crea una fila; sin esta limpieza la tabla creceria sin limite y las
 * consultas se irian degradando con el tiempo.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenService refreshTokenService;
    private final VerificationCodeService verificationCodeService;

    /**
     * Borra los tokens caducados o revocados una vez al dia, de madrugada.
     *
     * <p>La expresion cron se lee: segundo 0, minuto 0, hora 3, todos los dias.</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredTokens() {
        int deleted = refreshTokenService.purgeExpired();
        if (deleted > 0) {
            log.info("Limpieza programada: {} tokens de refresco eliminados", deleted);
        }

        int codigos = verificationCodeService.purgeExpired();
        if (codigos > 0) {
            log.info("Limpieza programada: {} codigos de verificacion eliminados", codigos);
        }
    }
}

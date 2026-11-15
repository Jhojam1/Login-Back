package com.proyecto.auth.service.impl;

import com.proyecto.auth.config.SecurityProperties;
import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.repository.UserRepository;
import com.proyecto.auth.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Implementacion de la politica de bloqueo por intentos fallidos.
 *
 * <p>El contador vive en la tabla de usuarios y no en memoria: asi el bloqueo sigue
 * vigente aunque la aplicacion se reinicie o aunque haya varias instancias EC2
 * detras de un balanceador.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    /**
     * Suma un intento fallido y bloquea la cuenta al alcanzar el limite configurado.
     *
     * <p>Se ejecuta en una transaccion propia ({@code REQUIRES_NEW}) porque el login
     * fallido termina lanzando una excepcion: si compartiera transaccion con el login,
     * el incremento del contador se desharia con el rollback y el bloqueo nunca
     * llegaria a producirse.</p>
     *
     * @param identifier usuario o correo con el que se intento entrar
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(String identifier) {
        userRepository.findByUsernameOrEmail(identifier).ifPresent(user -> {
            if (user.isAccountLocked()) {
                return;
            }

            user.setFailedAttempts(user.getFailedAttempts() + 1);

            if (user.getFailedAttempts() >= securityProperties.getMaxFailedAttempts()) {
                user.setAccountLocked(true);
                user.setLockTime(Instant.now());
                log.warn("Cuenta {} bloqueada tras {} intentos fallidos",
                        user.getUsername(), user.getFailedAttempts());
            }

            userRepository.save(user);
        });
    }

    /**
     * Reinicia el contador y guarda la fecha del acceso correcto.
     *
     * @param user usuario que acaba de autenticarse
     */
    @Override
    @Transactional
    public void registerSuccess(User user) {
        user.registerSuccessfulLogin();
        userRepository.save(user);
    }

    /**
     * Desbloquea la cuenta si ya transcurrio el tiempo de penalizacion.
     *
     * @param user usuario a revisar
     * @return {@code true} si la cuenta quedo desbloqueada en esta llamada
     */
    @Override
    @Transactional
    public boolean unlockIfTimeExpired(User user) {
        if (!user.isAccountLocked() || user.getLockTime() == null) {
            return false;
        }

        Instant unlockAt = user.getLockTime()
                .plus(Duration.ofMinutes(securityProperties.getLockDurationMinutes()));

        if (Instant.now().isBefore(unlockAt)) {
            return false;
        }

        user.setAccountLocked(false);
        user.setLockTime(null);
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("Cuenta {} desbloqueada automaticamente", user.getUsername());
        return true;
    }
}

package com.proyecto.auth.service.impl;

import com.proyecto.auth.config.MfaProperties;
import com.proyecto.auth.domain.entity.VerificationCode;
import com.proyecto.auth.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lleva la cuenta de los intentos fallidos sobre un codigo de verificacion.
 *
 * <p>Existe como clase aparte por un motivo concreto. Comprobar un codigo termina
 * lanzando una excepcion cuando falla, y una excepcion hace que Spring deshaga la
 * transaccion entera: el incremento del contador se perderia y el limite de intentos
 * no serviria de nada. Con {@code REQUIRES_NEW} este metodo se ejecuta en su propia
 * transaccion, que se confirma aunque la de fuera se deshaga.</p>
 *
 * <p>Tiene que estar en otra clase, no basta con anotar un metodo privado: Spring
 * aplica las transacciones mediante un proxy, y una llamada interna dentro del mismo
 * objeto no pasa por el. Es el mismo motivo por el que el contador de contrasenas
 * fallidas vive en {@code LoginAttemptServiceImpl}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationAttemptRecorder {

    private final VerificationCodeRepository verificationCodeRepository;
    private final MfaProperties mfaProperties;

    /**
     * Suma un intento fallido y, si se agotan, deja el codigo inservible.
     *
     * @param verificationCodeId identificador del codigo sobre el que se fallo
     * @return intentos que quedan; 0 si ya no queda ninguno
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(Long verificationCodeId) {
        VerificationCode codigo = verificationCodeRepository.findById(verificationCodeId)
                .orElse(null);

        if (codigo == null) {
            return 0;
        }

        codigo.setAttempts(codigo.getAttempts() + 1);
        int restantes = mfaProperties.getMaxAttempts() - codigo.getAttempts();

        if (restantes <= 0) {
            // Se quema el codigo: a partir de aqui ni el correcto vale, hay que
            // volver a empezar el inicio de sesion.
            codigo.setConsumed(true);
            log.warn("Codigo de verificacion invalidado tras {} intentos fallidos",
                    codigo.getAttempts());
        }

        verificationCodeRepository.save(codigo);

        return Math.max(restantes, 0);
    }
}

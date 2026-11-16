package com.proyecto.auth.service;

import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.domain.entity.VerificationCode;

/**
 * Contrato del ciclo de vida de los codigos de verificacion en dos pasos.
 */
public interface VerificationCodeService {

    /**
     * Genera un codigo, lo guarda cifrado y lo envia al correo del usuario.
     *
     * <p>Invalida cualquier codigo anterior de esa persona: tener varios validos a la
     * vez multiplicaria las posibilidades de acertar probando al azar.</p>
     *
     * @param user      usuario que esta intentando entrar
     * @param ipAddress IP de origen, para auditoria
     * @param userAgent cliente de origen, para auditoria
     * @return el codigo guardado, de donde se saca el identificador del intento
     */
    VerificationCode createAndSend(User user, String ipAddress, String userAgent);

    /**
     * Comprueba el codigo que ha escrito la persona.
     *
     * @param challengeId identificador del intento de inicio de sesion
     * @param code        codigo introducido
     * @return el usuario, si el codigo era correcto
     * @throws com.proyecto.auth.exception.BusinessException si no lo era, caduco o se agotaron los intentos
     */
    User verify(String challengeId, String code);

    /**
     * Vuelve a enviar un codigo para un intento que sigue en curso.
     *
     * @param challengeId identificador del intento
     * @param ipAddress   IP de origen
     * @param userAgent   cliente de origen
     * @return el codigo nuevo
     */
    VerificationCode resend(String challengeId, String ipAddress, String userAgent);

    /**
     * Borra los codigos caducados o ya usados.
     *
     * @return numero de registros eliminados
     */
    int purgeExpired();
}

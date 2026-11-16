package com.proyecto.auth.service.impl;

import com.proyecto.auth.config.MfaProperties;
import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.domain.entity.VerificationCode;
import com.proyecto.auth.exception.BusinessException;
import com.proyecto.auth.repository.VerificationCodeRepository;
import com.proyecto.auth.service.VerificationCodeService;
import com.proyecto.auth.service.notification.VerificationCodeSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Implementacion de la generacion y comprobacion de codigos de verificacion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    /** Generador criptografico: {@code Math.random()} seria predecible. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Bytes aleatorios del identificador publico del intento. */
    private static final int CHALLENGE_BYTES = 32;

    private final VerificationCodeRepository verificationCodeRepository;
    private final VerificationCodeSender sender;
    private final PasswordEncoder passwordEncoder;
    private final VerificationAttemptRecorder attemptRecorder;
    private final MfaProperties mfaProperties;

    /**
     * Genera un codigo nuevo, invalida los anteriores y lo manda por correo.
     *
     * @param user      usuario que esta intentando entrar
     * @param ipAddress IP de origen
     * @param userAgent cliente de origen
     * @return el codigo guardado
     */
    @Override
    @Transactional
    public VerificationCode createAndSend(User user, String ipAddress, String userAgent) {
        verificationCodeRepository.consumeAllForUser(user);

        String code = generateCode();

        VerificationCode verification = verificationCodeRepository.save(VerificationCode.builder()
                .challengeId(generateChallengeId())
                // Se guarda el resumen, nunca el codigo en claro
                .codeHash(passwordEncoder.encode(code))
                .user(user)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(mfaProperties.getExpirationMinutes())))
                .ipAddress(ipAddress)
                .userAgent(truncate(userAgent))
                .build());

        // El envio va despues de guardar: si fallara, el codigo queda registrado pero
        // nadie lo recibe, y la persona puede pedir un reenvio. Al reves (enviar y
        // luego fallar al guardar) recibiria un codigo que el sistema no reconoce.
        sender.send(user.getEmail(), code);

        return verification;
    }

    /**
     * Comprueba el codigo introducido.
     *
     * <p>Cada fallo suma un intento. Al agotarlos el codigo queda inservible y hay que
     * empezar de nuevo, que es lo que impide probar el millon de combinaciones.</p>
     *
     * @param challengeId identificador del intento
     * @param code        codigo introducido
     * @return el usuario, si el codigo era correcto
     * @throws BusinessException si el codigo no sirve
     */
    @Override
    @Transactional
    public User verify(String challengeId, String code) {
        VerificationCode verification = verificationCodeRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> invalido("La sesion de verificacion no existe. Vuelve a iniciar sesion."));

        if (verification.isConsumed()) {
            throw invalido("Ese codigo ya se uso. Vuelve a iniciar sesion.");
        }
        if (verification.isExpired()) {
            throw invalido("El codigo ha caducado. Vuelve a iniciar sesion.");
        }
        if (verification.getAttempts() >= mfaProperties.getMaxAttempts()) {
            throw invalido("Demasiados intentos fallidos. Vuelve a iniciar sesion.");
        }

        if (!passwordEncoder.matches(code.trim(), verification.getCodeHash())) {
            // El contador se actualiza en una transaccion aparte: esta de aqui se va a
            // deshacer en cuanto se lance la excepcion de abajo.
            int restantes = attemptRecorder.recordFailure(verification.getId());

            if (restantes <= 0) {
                throw invalido("Demasiados intentos fallidos. Vuelve a iniciar sesion.");
            }
            throw invalido("Codigo incorrecto. Te quedan %d intentos.".formatted(restantes));
        }

        // Correcto: se marca como usado para que no valga una segunda vez
        verification.setConsumed(true);
        verificationCodeRepository.save(verification);

        return verification.getUser();
    }

    /**
     * Reenvia un codigo nuevo para un intento que sigue abierto.
     *
     * @param challengeId identificador del intento
     * @param ipAddress   IP de origen
     * @param userAgent   cliente de origen
     * @return el codigo nuevo
     * @throws BusinessException si el intento ya no vale o se pide demasiado pronto
     */
    @Override
    @Transactional
    public VerificationCode resend(String challengeId, String ipAddress, String userAgent) {
        VerificationCode anterior = verificationCodeRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> invalido("La sesion de verificacion no existe. Vuelve a iniciar sesion."));

        if (anterior.isConsumed() || anterior.isExpired()) {
            throw invalido("Esa verificacion ya no esta activa. Vuelve a iniciar sesion.");
        }

        // Espera minima entre envios, para no inundar el buzon ni agotar la cuota de SES
        Instant puedeReenviarDesde = anterior.getCreatedAt()
                .plus(Duration.ofSeconds(mfaProperties.getResendCooldownSeconds()));

        if (Instant.now().isBefore(puedeReenviarDesde)) {
            long faltan = Duration.between(Instant.now(), puedeReenviarDesde).toSeconds() + 1;
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_TOO_SOON",
                    "Espera %d segundos antes de pedir otro codigo.".formatted(faltan));
        }

        return createAndSend(anterior.getUser(), ipAddress, userAgent);
    }

    /**
     * Borra los codigos caducados o consumidos.
     *
     * @return numero de registros eliminados
     */
    @Override
    @Transactional
    public int purgeExpired() {
        return verificationCodeRepository.deleteExpiredAndConsumed(Instant.now());
    }

    /**
     * Genera el codigo numerico que se envia por correo.
     *
     * @return una cadena de digitos con la longitud configurada
     */
    private String generateCode() {
        StringBuilder codigo = new StringBuilder(mfaProperties.getCodeLength());
        for (int i = 0; i < mfaProperties.getCodeLength(); i++) {
            codigo.append(SECURE_RANDOM.nextInt(10));
        }
        return codigo.toString();
    }

    /**
     * Genera el identificador publico del intento de inicio de sesion.
     *
     * @return una cadena aleatoria imposible de adivinar
     */
    private String generateChallengeId() {
        byte[] bytes = new byte[CHALLENGE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Construye el error de codigo invalido.
     *
     * @param mensaje texto para la persona
     * @return la excepcion lista para lanzar
     */
    private BusinessException invalido(String mensaje) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_VERIFICATION_CODE", mensaje);
    }

    /**
     * Recorta el user-agent al tamano de la columna.
     *
     * @param userAgent cabecera recibida, puede ser nula
     * @return la cadena recortada, o {@code null}
     */
    private String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }
}

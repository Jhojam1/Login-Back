package com.proyecto.auth.repository;

import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.domain.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Acceso a datos de los codigos de verificacion en dos pasos.
 */
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * Busca un codigo por el identificador publico del intento de inicio de sesion.
     *
     * @param challengeId valor que recibio el navegador tras acertar la contrasena
     * @return el codigo si existe
     */
    Optional<VerificationCode> findByChallengeId(String challengeId);

    /**
     * Da por consumidos todos los codigos pendientes de un usuario.
     *
     * <p>Se llama al generar uno nuevo: si quedaran varios validos a la vez, pedir el
     * reenvio varias veces multiplicaria las oportunidades de acertar al azar.</p>
     *
     * @param user usuario afectado
     * @return numero de codigos invalidados
     */
    @Modifying
    @Query("UPDATE VerificationCode v SET v.consumed = true WHERE v.user = :user AND v.consumed = false")
    int consumeAllForUser(@Param("user") User user);

    /**
     * Borra los codigos que ya no sirven.
     *
     * @param now instante de referencia
     * @return numero de filas eliminadas
     */
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :now OR v.consumed = true")
    int deleteExpiredAndConsumed(@Param("now") Instant now);
}

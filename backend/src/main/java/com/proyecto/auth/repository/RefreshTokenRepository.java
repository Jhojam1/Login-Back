package com.proyecto.auth.repository;

import com.proyecto.auth.domain.entity.RefreshToken;
import com.proyecto.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Acceso a datos de la entidad {@link RefreshToken}.
 *
 * <p>Incluye operaciones masivas de revocacion y limpieza, que se ejecutan con
 * {@code @Modifying} para no cargar cada fila en memoria.</p>
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca un token de refresco por su valor.
     *
     * @param token valor opaco recibido del cliente
     * @return el token si existe en la base de datos
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Revoca de golpe todos los tokens activos de un usuario.
     *
     * <p>Se usa al cerrar sesion en todos los dispositivos, al cambiar la contrasena
     * o cuando un administrador desactiva la cuenta.</p>
     *
     * @param user usuario cuyos tokens se invalidan
     * @return numero de tokens revocados
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllByUser(@Param("user") User user);

    /**
     * Elimina los tokens que ya expiraron o fueron revocados.
     *
     * <p>Lo invoca una tarea programada para que la tabla no crezca indefinidamente.</p>
     *
     * @param now instante de referencia (normalmente "ahora")
     * @return numero de filas borradas
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredAndRevoked(@Param("now") Instant now);

    /**
     * Cuenta las sesiones activas (tokens vigentes) de un usuario.
     *
     * @param user usuario a consultar
     * @param now  instante de referencia
     * @return numero de sesiones abiertas
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveSessions(@Param("user") User user, @Param("now") Instant now);
}

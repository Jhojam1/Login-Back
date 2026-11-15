package com.proyecto.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Utilidad para obtener los datos de origen de una peticion HTTP (IP y navegador),
 * que se guardan junto a cada token de refresco con fines de auditoria.
 */
@Component
public class RequestMetadataExtractor {

    /** Cabecera estandar que anaden los balanceadores y proxies inversos. */
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Obtiene la IP real del cliente.
     *
     * <p>Cuando la aplicacion esta detras de un balanceador de AWS o de Nginx, la IP de
     * la conexion es la del proxy, no la de la persona. La IP original llega en la
     * cabecera {@code X-Forwarded-For}, cuyo primer valor es el cliente inicial.</p>
     *
     * @param request peticion entrante
     * @return la direccion IP del cliente
     */
    public String extractIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Obtiene el identificador del navegador o cliente que hizo la peticion.
     *
     * @param request peticion entrante
     * @return el valor de la cabecera User-Agent, o "desconocido" si no viene
     */
    public String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.hasText(userAgent) ? userAgent : "desconocido";
    }
}

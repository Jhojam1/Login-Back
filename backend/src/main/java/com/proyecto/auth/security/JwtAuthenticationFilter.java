package com.proyecto.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que se ejecuta una vez por peticion y convierte un JWT valido en una sesion
 * autenticada dentro del contexto de Spring Security.
 *
 * <p>Flujo: lee la cabecera {@code Authorization: Bearer ...}, valida la firma y la
 * expiracion, carga el usuario y lo coloca en el {@code SecurityContextHolder}. A partir
 * de ese momento las anotaciones {@code @PreAuthorize} de los controladores ya saben
 * quien es quien.</p>
 *
 * <p>Si no hay token o es invalido, el filtro NO corta la cadena: simplemente deja la
 * peticion como anonima y sera la configuracion de rutas quien decida si eso basta.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Nombre de la cabecera HTTP donde viaja el token. */
    private static final String AUTH_HEADER = "Authorization";
    /** Prefijo obligatorio del valor de la cabecera. */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Procesa cada peticion HTTP intentando autenticarla mediante el JWT recibido.
     *
     * @param request     peticion entrante
     * @param response    respuesta en construccion
     * @param filterChain resto de filtros de la cadena
     * @throws ServletException si falla algun filtro posterior
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)
                && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtTokenProvider.isTokenValid(token)) {

            authenticate(token, request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token de la cabecera {@code Authorization}.
     *
     * @param request peticion entrante
     * @return el token sin el prefijo "Bearer ", o {@code null} si la cabecera no existe
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * Carga el usuario del token y lo registra como autenticado en el contexto.
     *
     * <p>Se vuelve a consultar la base de datos en lugar de confiar ciegamente en los
     * claims para que un usuario desactivado o con roles recien revocados deje de tener
     * acceso de inmediato, sin esperar a que caduque su token.</p>
     *
     * @param token   JWT ya verificado
     * @param request peticion, de la que se toman IP y user-agent para auditoria
     */
    private void authenticate(String token, HttpServletRequest request) {
        try {
            String username = jwtTokenProvider.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                log.debug("Token valido pero la cuenta {} esta deshabilitada o bloqueada", username);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            log.debug("No se pudo autenticar la peticion con el token recibido: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}

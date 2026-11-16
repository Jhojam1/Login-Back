package com.proyecto.auth.config;

import com.proyecto.auth.security.CustomUserDetailsService;
import com.proyecto.auth.security.JwtAccessDeniedHandler;
import com.proyecto.auth.security.JwtAuthenticationEntryPoint;
import com.proyecto.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion central de Spring Security.
 *
 * <p>Define tres cosas: quien puede entrar sin autenticarse, como se cifran las
 * contrasenas y en que punto de la cadena se inserta el filtro de JWT.</p>
 *
 * <p>La API es <b>sin estado</b> ({@code STATELESS}): no se crean sesiones HTTP ni
 * cookies de sesion. Cada peticion se autentica por si misma con su token, que es lo
 * que permite que el servicio escale horizontalmente en varias instancias EC2 sin
 * necesidad de compartir sesiones.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
public class SecurityConfig {

    /** Rutas publicas que no requieren token alguno. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/introspect",
            "/api/v1/auth/session-status",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityProperties securityProperties;

    /**
     * Define la cadena de filtros de seguridad y las reglas de acceso por ruta.
     *
     * @param http constructor de configuracion que proporciona Spring Security
     * @return la cadena de filtros ya configurada
     * @throws Exception si la configuracion es invalida
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF se desactiva porque no usamos cookies de sesion: el token viaja
                // en una cabecera, que un sitio de terceros no puede fijar por su cuenta.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Todo lo demas exige un token valido; el detalle de roles y
                        // permisos se declara con @PreAuthorize en cada controlador,
                        // que es donde resulta legible junto al metodo que protege.
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Proveedor de autenticacion que valida usuario y contrasena contra la base de datos.
     *
     * @return proveedor configurado con el servicio de usuarios y el cifrador
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Evita responder distinto cuando el usuario no existe, lo que permitiria
        // a un atacante descubrir que cuentas estan registradas.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    /**
     * Expone el gestor de autenticacion para poder inyectarlo en el servicio de login.
     *
     * @param configuration configuracion interna de Spring Security
     * @return el gestor de autenticacion
     * @throws Exception si no se puede construir
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Cifrador de contrasenas.
     *
     * <p>BCrypt incorpora una sal aleatoria por contrasena y es deliberadamente lento,
     * lo que encarece muchisimo un ataque por fuerza bruta sobre la base de datos.</p>
     *
     * @return el codificador BCrypt con el coste configurado
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(securityProperties.getBcryptStrength());
    }

    /**
     * Politica CORS que permite que el frontend (servido en otro dominio o puerto)
     * llame a esta API desde el navegador.
     *
     * @return la fuente de configuracion CORS aplicada a todas las rutas
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(securityProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Service-Key", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

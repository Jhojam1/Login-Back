package com.proyecto.auth.security;

import com.proyecto.auth.config.JwtProperties;
import com.proyecto.auth.domain.entity.Permission;
import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la emision y lectura de tokens JWT.
 *
 * <p>Son pruebas unitarias puras: no levantan el contexto de Spring ni la base de datos,
 * asi que se ejecutan en milisegundos. Es posible porque {@code JwtTokenProvider} solo
 * depende de {@code JwtProperties}, que aqui se construye a mano.</p>
 */
class JwtTokenProviderTest {

    /** Secreto de prueba; debe tener al menos 32 caracteres para HMAC-SHA256. */
    private static final String TEST_SECRET = "secreto-de-pruebas-con-longitud-suficiente-1234567890";

    private JwtTokenProvider jwtTokenProvider;
    private UserPrincipal principal;

    /**
     * Prepara un proveedor de tokens y un usuario de prueba antes de cada test.
     */
    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_SECRET);
        properties.setIssuer("auth-service-test");
        properties.setAccessTokenExpirationMs(900_000L);

        jwtTokenProvider = new JwtTokenProvider(properties);
        principal = new UserPrincipal(buildUser());
    }

    /**
     * Construye un usuario con un rol y dos permisos.
     *
     * @return usuario de prueba
     */
    private User buildUser() {
        Permission read = Permission.builder().id(1L).name("USER_READ").build();
        Permission upload = Permission.builder().id(2L).name("FILE_UPLOAD").build();

        Role role = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .permissions(Set.of(read, upload))
                .build();

        return User.builder()
                .id(42L)
                .username("jhojam")
                .email("jhojam@correo.com")
                .password("$2a$12$hashFalsoParaPruebas")
                .enabled(true)
                .roles(Set.of(role))
                .build();
    }

    @Test
    @DisplayName("El token emitido lleva el usuario, su id y su correo")
    void elTokenLlevaLaIdentidadDelUsuario() {
        String token = jwtTokenProvider.generateAccessToken(principal);

        Claims claims = jwtTokenProvider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("jhojam");
        assertThat(claims.get(JwtTokenProvider.CLAIM_USER_ID, Integer.class)).isEqualTo(42);
        assertThat(claims.get(JwtTokenProvider.CLAIM_EMAIL, String.class)).isEqualTo("jhojam@correo.com");
        assertThat(claims.getIssuer()).isEqualTo("auth-service-test");
    }

    @Test
    @DisplayName("El token lleva los roles y los permisos, que es lo que permite autorizar sin consultar la base de datos")
    void elTokenLlevaRolesYPermisos() {
        String token = jwtTokenProvider.generateAccessToken(principal);
        Claims claims = jwtTokenProvider.parseClaims(token);

        assertThat(jwtTokenProvider.extractStringSet(claims, JwtTokenProvider.CLAIM_ROLES))
                .containsExactly("ROLE_USER");

        assertThat(jwtTokenProvider.extractStringSet(claims, JwtTokenProvider.CLAIM_PERMISSIONS))
                .containsExactlyInAnyOrder("USER_READ", "FILE_UPLOAD");
    }

    @Test
    @DisplayName("Un token recien emitido es valido")
    void unTokenRecienEmitidoEsValido() {
        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("jhojam");
    }

    @Test
    @DisplayName("Un token con la firma manipulada se rechaza")
    void unTokenManipuladoSeRechaza() {
        String token = jwtTokenProvider.generateAccessToken(principal);

        // Se altera el ultimo caracter de la firma: basta eso para invalidarlo.
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtTokenProvider.isTokenValid(tampered)).isFalse();
        assertThat(jwtTokenProvider.tryParseClaims(tampered)).isEmpty();
    }

    @Test
    @DisplayName("Un token firmado con otro secreto se rechaza")
    void unTokenFirmadoConOtroSecretoSeRechaza() {
        String tokenAjeno = jwtTokenProvider.generateAccessToken(principal);

        JwtProperties otrasPropiedades = new JwtProperties();
        otrasPropiedades.setSecret("otro-secreto-completamente-distinto-0987654321");
        otrasPropiedades.setIssuer("auth-service-test");
        JwtTokenProvider otroProveedor = new JwtTokenProvider(otrasPropiedades);

        assertThat(otroProveedor.isTokenValid(tokenAjeno)).isFalse();
    }

    @Test
    @DisplayName("Un token ya caducado se rechaza")
    void unTokenCaducadoSeRechaza() {
        JwtProperties caducadas = new JwtProperties();
        caducadas.setSecret(TEST_SECRET);
        caducadas.setIssuer("auth-service-test");
        // El minimo que admite la validacion es 60 s; se resta para que nazca vencido.
        caducadas.setAccessTokenExpirationMs(-1_000L);

        String tokenVencido = new JwtTokenProvider(caducadas).generateAccessToken(principal);

        assertThat(jwtTokenProvider.isTokenValid(tokenVencido)).isFalse();
        assertThat(jwtTokenProvider.tryParseClaims(tokenVencido)).isEmpty();
    }

    @Test
    @DisplayName("Una cadena que no es un JWT se rechaza sin lanzar excepcion")
    void unaCadenaCualquieraSeRechaza() {
        assertThat(jwtTokenProvider.isTokenValid("esto.no.es.un.token")).isFalse();
        assertThat(jwtTokenProvider.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("Las autoridades incluyen el rol con prefijo y los permisos sin prefijo")
    void lasAutoridadesIncluyenRolesYPermisos() {
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_USER", "USER_READ", "FILE_UPLOAD");
    }
}

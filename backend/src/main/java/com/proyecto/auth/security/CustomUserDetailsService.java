package com.proyecto.auth.security;

import com.proyecto.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puente entre Spring Security y la base de datos de usuarios.
 *
 * <p>Spring Security llama a este servicio cuando necesita cargar un usuario, tanto al
 * validar un login como al reconstruir el contexto a partir de un JWT.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga un usuario aceptando indistintamente su nombre de usuario o su correo.
     *
     * @param identifier username o email escrito en el formulario de login
     * @return el usuario envuelto en un {@link UserPrincipal}
     * @throws UsernameNotFoundException si no existe ningun usuario con ese identificador
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe ningun usuario con el identificador: " + identifier));
    }
}

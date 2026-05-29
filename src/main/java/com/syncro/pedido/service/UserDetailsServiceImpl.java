package com.syncro.pedido.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.syncro.pedido.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;


/**
 * Implementación de UserDetailsService requerida por Spring Security.
 *
 * ¿Para qué sirve? Spring Security necesita saber cómo cargar un usuario desde
 * la BD dado su nombre de usuario (en nuestro caso, el email). Esta clase le
 * dice exactamente cómo hacerlo.
 *
 * ¿Por qué está separada de SecurityConfig? Para evitar una dependencia
 * circular: JwtAuthFilter → UserDetailsService → SecurityConfig → JwtAuthFilter
 * ❌
 *
 * Al tenerla en su propia clase @Service, Spring puede instanciarla de forma
 * independiente sin crear el ciclo: JwtAuthFilter → UserDetailsServiceImpl ✅
 * SecurityConfig → UserDetailsServiceImpl ✅
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Carga el usuario desde la base de datos usando su email. Spring Security
     * llama a este método automáticamente durante: - La validación del token
     * JWT en JwtAuthFilter - El proceso de login en AuthenticationManager
     *
     * param email identificador del usuario (usado como username)
     * return UserDetails con los datos del usuario para Spring Security
     * throws UsernameNotFoundException si no existe el usuario → HTTP 401
     */

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                "No se encontró usuario con email: " + email));
    }

}

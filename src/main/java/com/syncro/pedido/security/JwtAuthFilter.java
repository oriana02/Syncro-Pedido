package com.syncro.pedido.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro que intercepta cada petición HTTP y valida el token JWT.
 *
 * ¿Cómo funciona? Antes de que la petición llegue al controller, este filtro:
 * 1. Lee el header "Authorization: Bearer <token>" 2. Extrae el email del token
 * 3. Carga el usuario desde la BD 4. Verifica que el token sea válido y no haya
 * expirado 5. Si todo es correcto, registra al usuario como autenticado en el
 * contexto de Spring Security
 *
 * Extiende OncePerRequestFilter para garantizar que el filtro se ejecute
 * exactamente UNA vez por petición (no duplicado).
 *
 * Si el token es inválido o ausente, la petición continúa sin autenticación y
 * Spring Security la rechaza con 401 en las rutas protegidas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    //utilidad para extraer y validar datos del token jwt
    private final JwtUtil jwtUtil;

    // servicio que carga el usuario desde la DB por su email 
    private final UserDetailsService userDetailsService;

    /**
     * Lógica principal del filtro. Se ejecuta en cada petición HTTP.
     *
     * param request petición HTTP entrant param response respuesta HTTP
     * saliente param filterChain cadena de filtros: llama al siguiente filtro o
     * al controller
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Paso 1: leer el header de autorización
        final String authHeader = request.getHeader("Authorization");

        // Si no tiene header o no empieza con "Bearer ", dejar pasar sin autenticar.
        // Spring Security rechazará la petición si la ruta está protegida.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Petición sin token JWT: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Paso 2: extraer el token quitando el prefijo "Bearer "
        final String token = authHeader.substring(7);

        try {
            // Paso 3: extraer el email del payload del token
            final String email = jwtUtil.extractUsername(token);

            // Solo autenticar si hay email y el usuario no está ya autenticado en esta petición
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Paso 4: cargar el usuario completo desde la base de datos
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Paso 5: verificar que el token sea válido para este usuario
                if (jwtUtil.validateToken(token, userDetails)) {

                    // Paso 6: crear el objeto de autenticación con el usuario y sus roles
                    UsernamePasswordAuthenticationToken authToken
                            = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // credenciales (null porque ya está autenticado)
                                    userDetails.getAuthorities() // roles del usuario (ADMIN, OPERADOR, etc.)
                            );

                    // Agregar detalles de la petición (IP, session ID) al contexto
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Paso 7: registrar la autenticación en el contexto de Spring Security
                    // A partir de aquí, Spring sabe quién es el usuario en esta petición
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Usuario '{}' autenticado correctamente", email);

                } else {
                    log.warn("Token JWT inválido o expirado para el usuario: {}", email);
                }
            }

        } catch (Exception e) {
            // Si el token está malformado, manipulado o expirado, simplemente se ignora.
            // Spring Security rechazará la petición con 401 si la ruta está protegida.
            log.warn("Error al procesar token JWT en [{}]: {}", request.getRequestURI(), e.getMessage());
        }

        // Paso 8: continuar con el siguiente filtro o el controller
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs")
                || path.equals("/favicon.ico");
    }

}

package com.syncro.pedido.service;

import org.springframework.stereotype.Service;

import com.syncro.pedido.dto.request.LoginRequest;
import com.syncro.pedido.dto.request.RegisterRequest;
import com.syncro.pedido.dto.response.AuthResponse;
import com.syncro.pedido.model.Empresa;
import com.syncro.pedido.model.Rol;
import com.syncro.pedido.model.Usuario;
import com.syncro.pedido.repository.EmpresaRepository;
import com.syncro.pedido.repository.UsuarioRepository;
import com.syncro.pedido.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    private final EmpresaRepository empresaRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    /**
     * Registra un nuevo operador en el sistema.
     *
     * Pasos que realiza: 1. Verifica que el email no esté ya registrado 2.
     * Verifica que la empresa exista 3. Cifra la contraseña con BCrypt 4.
     * Guarda el usuario en la BD 5. Genera y devuelve el token JWT
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registrando nuevo usuario con email={}", request.getEmail());

        // Paso 1: verificar que el email no esté en uso
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con el email: " + request.getEmail());
        }

        // Paso 2: verificar que la empresa exista
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                "No existe empresa con ID: " + request.getEmpresaId()));

        // Paso 3: construir el usuario con la contraseña cifrada
        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                // Cifrar la contraseña con BCrypt antes de guardar
                .password(passwordEncoder.encode(request.getPassword()))
                .empresa(empresa)
                // Si no se envía rol, asignar OPERADOR por defecto
                .rol(request.getRol() != null ? request.getRol() : Rol.OPERADOR)
                .activo(true)
                .build();

        // Paso 4: guardar en la BD
        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario ID={} registrado exitosamente", guardado.getId());

        // Paso 5: generar el token JWT y construir la respuesta
        String token = jwtUtil.generateToken(guardado);

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .email(guardado.getEmail())
                .nombre(guardado.getNombre())
                .rol(guardado.getRol().name())
                .empresaId(empresa.getId())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }

    /**
     * Autentica un usuario con email y contraseña.
     *
     * Spring Security se encarga de: - Cargar el usuario desde la BD por email
     * - Comparar la contraseña con el hash BCrypt almacenado - Lanzar excepción
     * automáticamente si las credenciales son incorrectas
     *
     * Si la autenticación es exitosa, se genera y devuelve el token JWT.
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Intento de login para email={}", request.getEmail());

        // Spring Security valida las credenciales automáticamente.
        // Si son incorrectas, lanza BadCredentialsException → HTTP 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Si llegamos aquí, las credenciales son válidas
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(usuario);
        log.info("Login exitoso para usuario ID={}", usuario.getId());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .empresaId(usuario.getEmpresa().getId())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }

}

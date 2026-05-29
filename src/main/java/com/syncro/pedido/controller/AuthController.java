package com.syncro.pedido.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncro.pedido.dto.request.LoginRequest;
import com.syncro.pedido.dto.request.RegisterRequest;
import com.syncro.pedido.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.syncro.pedido.dto.response.AuthResponse;

/**
 * Controlador REST para autenticación de usuarios.
 *
 * Estas rutas son PÚBLICAS: no requieren token JWT. Están configuradas como
 * rutas abiertas en SecurityConfig.
 *
 * Base URL: /auth
 *
 * Endpoints disponibles: POST /auth/login → iniciar sesión, devuelve token JWT
 * POST /auth/register → registrar nuevo operador, devuelve token JWT
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // =========================================================================
    // POST /auth/login
    // =========================================================================
    /**
     * Inicia sesión con email y contraseña.
     *
     * Respuestas posibles: 200 OK → login exitoso, devuelve AuthResponse con
     * token 400 Bad Request → campos inválidos (email mal formado, password
     * vacío) 401 Unauthorized → credenciales incorrectas
     *
     * Ejemplo de body: { "email": "sofia@pyme-demo.cl", "password":
     * "Admin1234!" }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("POST /auth/login - email={}", request.getEmail());
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Registra un nuevo operador en el sistema.
     *
     * Crea el usuario en la BD con la contraseña cifrada (BCrypt) y devuelve
     * directamente un token JWT para que el usuario pueda operar sin necesidad
     * de hacer login por separado.
     *
     * Ejemplo de body: { "nombre": "Sofía Gómez", "email":
     * "sofia@pyme-demo.cl", "password": "Admin1234!", "empresaId": 1, "rol":
     * "OPERADOR" }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("POST /auth/register - email={}", request.getEmail());
        AuthResponse response = authService.register(request);

        // devulve 201 created porque se crep un nuevo recurso
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}

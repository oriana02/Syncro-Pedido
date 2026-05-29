package com.syncro.pedido.security;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.crypto.SecretKey;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import java.util.function.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

/**
 * Utilidad para generar y validar tokens JWT (JSON Web Token).
 *
 * ¿Qué es un JWT? Es un token firmado digitalmente que contiene información del
 * usuario (email, rol). El servidor lo genera al hacer login y el cliente lo
 * envía en cada petición para identificarse sin necesidad de sesión.
 *
 * Estructura del token: header.payload.signature - header: algoritmo de firma
 * (HS256) - payload: datos del usuario (email, rol, expiración) - signature:
 * firma digital que garantiza que nadie modificó el token
 *
 * Configuración en application.yml: jwt.secret: clave secreta para firmar
 * (mínimo 256 bits) jwt.expiration: tiempo de vida en milisegundos (86400000 =
 * 24h)
 */
@Component
@Slf4j
public class JwtUtil {

    /**
     * Clave secreta usada para firmar y verificar el token
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Tiempo de expiración del token en milisegundos
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Convierte el secret (String) en una clave criptográfica segura. Se usa el
     * algoritmo HMAC-SHA256 (HS256). La clave debe tener al menos 256 bits (32
     * caracteres).
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un token JWT para un usuario autenticado. El token incluye el rol
     * del usuario como claim adicional para que el Gateway pueda validar
     * permisos sin consultar la BD.
     *
     * param userDetails usuario autenticado por Spring Security return token
     * JWT firmado listo para enviar al cliente
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Incluir el rol en el token para que el frontend pueda usarlo
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList());

        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Construye y firma el token JWT con los datos del usuario.
     *
     * param claims datos adicionales a incluir en el payload (ej: roles) param
     * subject identificador del usuario (email) return token JWT como String
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject) // email del usuario
                .issuedAt(new Date()) // fecha de emisión
                .expiration(new Date(System.currentTimeMillis() + expiration)) // fecha de expiración
                .signWith(getSigningKey()) // firma digital
                .compact();
    }

    /**
     * Extrae el email (subject) del token. Se usa en JwtAuthFilter para
     * identificar al usuario en cada petición.
     *
     * param token JWT recibido en el header Authorization return email del
     * usuario
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración del token.
     *
     * param token JWT a analizar return fecha y hora en que vence el token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Método genérico para extraer cualquier dato (claim) del token. Se usa
     * internamente por extractUsername y extractExpiration.
     *
     * param token JWT a analizar param claimsResolver función que indica qué
     * dato extraer return el dato solicitado del payload del token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea y verifica la firma del token, devolviendo todos sus claims. Si la
     * firma no es válida o el token está malformado, lanza excepción.
     *
     * param token JWT a parsear return claims (payload) del token throws
     * JwtException si el token es inválido o fue manipulado
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // verifica que la firma sea correcta
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica si el token ya venció comparando su fecha de expiración con la
     * fecha y hora actual del servidor.
     *
     * param token JWT a verificar return true si el token expiró, false si
     * todavía es válido
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Valida que el token pertenezca al usuario y no haya expirado. Se llama
     * desde JwtAuthFilter en cada petición protegida.
     *
     * param token JWT enviado por el cliente param userDetails usuario cargado
     * desde la base de datos return true si el token es válido para ese usuario
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Devuelve el tiempo de expiración configurado en milisegundos. Se usa en
     * AuthResponse para informar al frontend cuándo vence el token.
     */
    public Long getExpiration() {
        return expiration;
    }
}

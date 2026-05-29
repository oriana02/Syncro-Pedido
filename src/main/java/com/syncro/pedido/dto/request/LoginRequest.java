package com.syncro.pedido.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


/**
 * Datos para iniciar sesión en la plataforma. Endpoint: POST /auth/login
 *
 * Ejemplo de JSON esperado: { "email": "sofia@pyme-demo.cl", "password":
 * "Admin1234!" }
 *
 * Si las credenciales son válidas, el servidor devuelve un AuthResponse con el
 * token JWT para usar en peticiones siguientes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Email registrado del operador. Se usa como nombre de usuario.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;

    /**
     * Contraseña en texto plano. Spring Security la compara contra el hash
     * BCrypt almacenado en BD. NUNCA se almacena ni se loguea en texto plano.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
}

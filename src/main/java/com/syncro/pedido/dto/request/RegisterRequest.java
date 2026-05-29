package com.syncro.pedido.dto.request;

import com.syncro.pedido.model.Rol;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Datos para registrar un nuevo operador en el sistema. 
 * Endpoint: POST
 * /auth/register
 *
 * Ejemplo de JSON esperado: 
 * { "nombre": "Sofía Gómez", "email":
 * "sofia@pyme-demo.cl", 
 * "password": "Admin1234!", 
 * "empresaId": 1, 
 * "rol":
 * "OPERADOR" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /**
     * Nombre completo del operador
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    /**
     * Email que usará para iniciar sesión. Debe ser único en el sistema.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    /**
     * Contraseña en texto plano. El servicio la cifrará con BCrypt antes de
     * guardarla en BD. Mínimo 6 caracteres.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;

    /**
     * ID de la empresa a la que pertenecerá este operador. Debe existir
     * previamente en la tabla empresa.
     */
    @NotNull(message = "El ID de la empresa es obligatorio")
    private Long empresaId;

    /**
     * Rol del nuevo operador dentro del sistema. Si no se envía, el servicio
     * asigna OPERADOR por defecto. Valores válidos: ADMIN, OPERADOR, BODEGUERO
     */
    private Rol rol;

}

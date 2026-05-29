package com.syncro.pedido.dto.response;

import lombok.*;

/**
 * Respuesta devuelta al cliente después de un login o registro exitoso.
 * Contiene el token JWT que el frontend debe guardar y enviar en cada petición
 * siguiente como header:
 *
 * Authorization: Bearer <token>
 *
 * Ejemplo de respuesta: { 
 * "token": "eyJhbGciOiJIUzI1NiJ9...", 
 * "tipo": "Bearer",
 * "email": "sofia@pyme-demo.cl", 
 * " nombre": "Sofía Gómez", 
 * " rol": "OPERADOR",
 * "empresaId": 1, 
 * "expiresIn": 86400000 }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AuthResponse {

    /**
     * Token JWT firmado que el cliente usa en peticiones posteriores
     */
    private String token;

    /**
     * Tipo de token. Siempre "Bearer" en este sistema. El cliente lo usa para
     * construir el header de autorización.
     */
    private String tipo;

    /**
     * Email del usuario autenticado
     */
    private String email;

    /**
     * Nombre completo del usuario autenticado
     */
    private String nombre;

    /**
     * Rol del usuario autenticado: ADMIN, OPERADOR o BODEGUERO. El frontend
     * puede usarlo para mostrar u ocultar opciones en la UI.
     */
    private String rol;

    /**
     * ID de la empresa a la que pertenece el usuario. El frontend lo usa para
     * filtrar pedidos por empresa automáticamente.
     */
    private Long empresaId;

    /**
     * Tiempo en milisegundos hasta que expira el token. Ejemplo: 86400000 = 24
     * horas. El frontend puede usarlo para saber cuándo debe pedir un nuevo
     * token.
     */
    private Long expiresIn;

}

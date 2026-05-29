package com.syncro.pedido.dto.response;

import lombok.*;

/**
 * Información resumida del operador para incluir dentro de la respuesta de un
 * pedido (PedidoResponse).
 *
 * Solo expone nombre e email del operador que creó el pedido. NUNCA se expone
 * la contraseña ni el hash de la misma.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UsuarioResumenResponse {

    /**
     * ID del usuario en la base de datos
     */
    private Long id;

    /**
     * Nombre completo del operador
     */
    private String nombre;

    /**
     * Email del operador
     */
    private String email;
}

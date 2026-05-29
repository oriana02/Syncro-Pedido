package com.syncro.pedido.dto.request;

import com.syncro.pedido.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos para cambiar el estado de un pedido. El frontend envía este JSON al
 * endpoint PATCH /pedidos/{id}/estado.
 *
 * Ejemplo de JSON esperado: { "nuevoEstado": "CONFIRMADO", "motivo": "Pago
 * verificado exitosamente" }
 *
 * Transiciones válidas (definidas en PedidoService): PENDIENTE → CONFIRMADO,
 * CANCELADO CONFIRMADO → EN_PREPARACION, CANCELADO EN_PREPARACION → DESPACHADO,
 * CANCELADO DESPACHADO → EN_RUTA EN_RUTA → ENTREGADO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambiarEstadoRequest {

    /**
     * Estado al que se quiere mover el pedido. Si la transición no es válida,
     * el servicio lanzará TransicionEstadoInvalidaException con error 409.
     */
    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoPedido nuevoEstado;

    /**
     * Explicación del cambio. Opcional pero recomendado para auditoría.
     */
    @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
    private String motivo;

}

package com.syncro.pedido.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.syncro.pedido.model.EstadoPedido;

/**
 * Respuesta resumida de un pedido para listados. 
 * Se devuelve en el endpoint GET
 * /pedidos/historial/{empresaId}.
 *
 * Contiene solo los campos esenciales para no sobrecargar la respuesta cuando
 * se listan muchos pedidos a la vez. 
 * Si se necesita el detalle completo de un
 * pedido, se usa PedidoResponse con GET /pedidos/{id}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResumenResponse {

    /**
     * ID único del pedido
     */
    private Long id;

    /**
     * Nombre de la empresa que realizó el pedido
     */
    private String empresaNombre;

    /**
     * Nombre del operador que creó el pedido
     */
    private String usuarioNombre;

    /**
     * Estado actual del pedido
     */
    private EstadoPedido estado;

    /**
     * Total del pedido (subtotal + costoEnvio)
     */
    private BigDecimal total;

    /**
     * Cantidad de ítems distintos en el pedido
     */
    private Integer totalItems;

    /**
     * Fecha en que se registró el pedido
     */
    private LocalDateTime fechaCreacion;

    /**
     * ID del pedido en plataforma externa (si aplica)
     */
    private String referenciaExterna;

}

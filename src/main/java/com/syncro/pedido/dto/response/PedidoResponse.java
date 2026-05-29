package com.syncro.pedido.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.syncro.pedido.model.EstadoPedido;

/**
 * Respuesta completa de un pedido con todos sus detalles. Se devuelve en los
 * endpoints: 
 * - POST /pedidos → al crear un pedido 
 * - GET /pedidos/{id} → al consultar un pedido por ID 
 * - PATCH /pedidos/{id}/estado → al cambiar el estado
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PedidoResponse {

    /**
     * ID único del pedido en la base de datos
     */
    private Long id;

    /**
     * Información resumida de la empresa que realizó el pedido
     */
    private EmpresaResumenResponse empresa;

    /**
     * Información resumida del operador que creó el pedido
     */
    private UsuarioResumenResponse usuario;

    /**
     * Dirección de entrega completa
     */
    private DireccionResponse direccion;

    /**
     * Estado actual del pedido en su ciclo de vida
     */
    private EstadoPedido estado;

    /**
     * Valor de los productos sin incluir el costo de envío
     */
    private BigDecimal subtotal;

    /**
     * Costo del despacho calculado por MS-Envíos
     */
    private BigDecimal costoEnvio;

    /**
     * Total final = subtotal + costoEnvio
     */
    private BigDecimal total;

    /**
     * Instrucciones o comentarios adicionales del operador
     */
    private String notas;

    /**
     * ID del pedido en plataforma externa como Shopify (si aplica)
     */
    private String referenciaExterna;

    /**
     * Fecha en que se registró el pedido
     */
    private LocalDateTime fechaCreacion;

    /**
     * Fecha de la última modificación del pedido
     */
    private LocalDateTime fechaActualizacion;

    /**
     * Fecha en que el pedido fue confirmado (null si aún no se confirma)
     */
    private LocalDateTime fechaConfirmacion;

    /**
     * Fecha en que el pedido fue entregado (null si aún no se entrega)
     */
    private LocalDateTime fechaEntrega;

    /**
     * Lista de productos incluidos en el pedido
     */
    private List<ItemPedidoResponse> items;

    /**
     * Historial completo de todos los cambios de estado. Permite trazabilidad
     * total: quién cambió, cuándo y por qué.
     */
    private List<HistorialEstadoResponse> historialEstados;

}

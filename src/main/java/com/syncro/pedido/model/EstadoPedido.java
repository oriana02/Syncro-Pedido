package com.syncro.pedido.model;

/**
 * Estados posibles de un pedido durante su ciclo de vida. Las transiciones
 * válidas entre estados se definen en PedidoService. Deben coincidir
 * exactamente con el CHECK constraint del SQL.
 *
 * Flujo normal: PENDIENTE → CONFIRMADO → EN_PREPARACION → DESPACHADO → EN_RUTA
 * → ENTREGADO
 *
 * Flujo de cancelación (disponible hasta EN_PREPARACION): PENDIENTE → CANCELADO
 * CONFIRMADO → CANCELADO EN_PREPARACION → CANCELADO
 */
public enum EstadoPedido {

    /**
     * Pedido registrado pero aún no validado ni confirmado
     */
    PENDIENTE,
    /**
     * Pedido validado y aceptado. Al llegar a este estado se publica el evento
     * "pedido.creado" en RabbitMQ, lo que dispara el descuento de stock en
     * MS-Inventario y la creación del despacho en MS-Envíos.
     */
    CONFIRMADO,
    /**
     * El pedido está siendo preparado en bodega
     */
    EN_PREPARACION,
    /**
     * El pedido salió de bodega y fue entregado al transportista
     */
    DESPACHADO,
    /**
     * El pedido está en camino hacia el cliente
     */
    EN_RUTA,
    /**
     * El pedido fue entregado exitosamente al cliente
     */
    ENTREGADO,
    /**
     * El pedido fue cancelado antes de ser despachado
     */
    CANCELADO
}

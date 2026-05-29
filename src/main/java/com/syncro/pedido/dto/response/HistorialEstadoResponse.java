package com.syncro.pedido.dto.response;

import lombok.*;

import java.time.LocalDateTime;

import com.syncro.pedido.model.EstadoPedido;

/**
 * Detalle de un cambio de estado en el historial del pedido. Permite saber
 * exactamente quién cambió el estado, cuándo y por qué.
 *
 * Es parte de la lista "historialEstados" dentro de PedidoResponse. Implementa
 * el requerimiento RF-1.2 de trazabilidad completa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class HistorialEstadoResponse {

    /**
     * ID del registro de historial
     */
    private Long id;

    /**
     * Estado en el que estaba el pedido ANTES del cambio
     */
    private EstadoPedido estadoAnterior;

    /**
     * Estado al que pasó el pedido DESPUÉS del cambio
     */
    private EstadoPedido estadoNuevo;

    /**
     * Fecha y hora exacta en que ocurrió el cambio
     */
    private LocalDateTime fechaCambio;

    /**
     * Tipo de actor que realizó el cambio: - USUARIO: un operador humano desde
     * la interfaz - SISTEMA: cambio automático por lógica interna - EVENTO:
     * cambio disparado por un evento de RabbitMQ
     */
    private String actorTipo;

    /**
     * ID del usuario que realizó el cambio. Null si el cambio fue automático
     * (SISTEMA o EVENTO).
     */
    private Long actorId;

    /**
     * Explicación del motivo del cambio (opcional)
     */
    private String motivo;
}

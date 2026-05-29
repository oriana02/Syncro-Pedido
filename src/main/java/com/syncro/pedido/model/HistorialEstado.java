package com.syncro.pedido.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registra cada cambio de estado que sufre un pedido. Permite trazabilidad
 * completa: se sabe exactamente quién cambió el estado, cuándo y por qué motivo
 * (RF-1.2).
 *
 * También se puede insertar automáticamente desde el trigger
 * trg_pedido_historial de Oracle cuando se actualiza el estado del pedido.
 *
 * Tabla: historial_estado_pedido
 */
@Entity
@Table(name = "historial_estado_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstado {

    /**
     * Clave primaria generada por la secuencia de Oracle
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Pedido al que pertenece este registro de historial
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /**
     * Estado en el que estaba el pedido ANTES del cambio. Puede ser null solo
     * en el registro inicial (creación del pedido).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoPedido estadoAnterior;

    /**
     * Estado al que pasó el pedido después del cambio
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoPedido estadoNuevo;

    /**
     * ID del usuario que realizó el cambio. Puede ser null si el cambio fue
     * automático (ej: trigger del sistema).
     */
    @Column(name = "actor_id")
    private Long actorId;

    /**
     * Tipo de actor que realizó el cambio: - USUARIO: un operador humano desde
     * la interfaz - SISTEMA: cambio automático por lógica interna - EVENTO:
     * cambio disparado por un evento de RabbitMQ
     */
    @Column(name = "actor_tipo", length = 20)
    @Builder.Default
    private String actorTipo = "USUARIO";

    /**
     * Explicación del motivo del cambio (opcional pero recomendado)
     */
    @Column(length = 300)
    private String motivo;

    /**
     * Fecha y hora exacta en que se realizó el cambio de estado
     */
    @Column(name = "fecha_cambio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCambio = LocalDateTime.now();

}

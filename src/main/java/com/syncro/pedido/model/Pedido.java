package com.syncro.pedido.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad central del microservicio. Representa un pedido realizado por un
 * operador de una PYME a través de la plataforma Syncro.
 *
 * Ciclo de vida del estado: PENDIENTE → CONFIRMADO → EN_PREPARACION →
 * DESPACHADO → EN_RUTA → ENTREGADO ↘ CANCELADO (desde cualquier estado antes de
 * DESPACHADO)
 *
 * Al pasar a CONFIRMADO, se publica el evento "pedido.creado" en RabbitMQ para
 * que MS-Inventario descuente el stock y MS-Envíos genere el despacho.
 *
 * Tabla: pedido
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    /**
     * Clave primaria generada por la secuencia de MYSQL
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Empresa que realizó este pedido. Se guarda como FK hacia la tabla
     * empresa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Usuario (operador) que creó el pedido dentro de la empresa. Se usa
     * también como "actor" en el historial de estados.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Dirección de entrega del pedido. Se guarda en tabla separada para
     * mantener el historial aunque el cliente cambie su dirección después.
     */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "direccion_id", nullable = false)
    private DireccionEntrega direccion;

    /**
     * Estado actual del pedido en su ciclo de vida. Las transiciones válidas se
     * validan en PedidoService.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    /**
     * Valor de los productos sin incluir el costo de envío. Se calcula sumando
     * los subtotales de cada ItemPedido.
     */
    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /**
     * Costo del despacho calculado por MS-Envíos. Inicialmente en 0 hasta que
     * se calcule el envío.
     */
    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    /**
     * Total final = subtotal + costoEnvio
     */
    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    /**
     * Instrucciones o comentarios adicionales del operador
     */
    @Column(length = 500)
    private String notas;

    /**
     * ID del pedido en el ecommerce externo (opcional). Permite trazabilidad
     * con plataformas de terceros como Shopify o WooCommerce.
     */
    @Column(length = 100)
    private String referenciaExterna;

    /**
     * Fecha en que se registró el pedido en el sistema
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /**
     * Fecha de la última modificación. Se actualiza automáticamente por el
     * trigger trg_pedido_updated_at en Oracle. También se actualiza desde Java
     * en @PreUpdate.
     */
    private LocalDateTime fechaActualizacion;

    /**
     * Se establece automáticamente cuando el pedido pasa a CONFIRMADO
     */
    private LocalDateTime fechaConfirmacion;

    /**
     * Se establece automáticamente cuando el pedido pasa a ENTREGADO
     */
    private LocalDateTime fechaEntrega;

    /**
     * Indica si ya se publicó el evento "pedido.creado" en RabbitMQ. Evita
     * publicar el evento dos veces si hay un reintento o fallo. false = aún no
     * publicado, true = ya publicado.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean eventoPublicado = false;

    /**
     * Lista de productos incluidos en este pedido. CascadeType.ALL: si se
     * elimina el pedido, se eliminan sus ítems. orphanRemoval: si se quita un
     * ítem de la lista, se borra de la BD.
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ItemPedido> items = new ArrayList<>();

    /**
     * Registro de todos los cambios de estado del pedido. Permite trazabilidad
     * completa: quién cambió el estado, cuándo y por qué.
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<HistorialEstado> historialEstados = new ArrayList<>();

    /**
     * Se ejecuta automáticamente antes de cada UPDATE en la base de datos.
     * Mantiene sincronizada la fecha de actualización desde el lado Java.
     */
    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

}

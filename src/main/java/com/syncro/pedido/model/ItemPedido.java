package com.syncro.pedido.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa una línea de producto dentro de un pedido. Cada ítem guarda un
 * "snapshot" del producto al momento del pedido: SKU, nombre y precio. Esto es
 * importante porque los precios pueden cambiar en MS-Inventario después de que
 * se realizó el pedido.
 *
 * Nota: el SKU referencia al producto en MS-Inventario, pero NO es una FK
 * cruzada entre microservicios (patrón Database per Service).
 *
 * Tabla: item_pedido
 */
@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    /**
     * Clave primaria generada por la secuencia de MYSQL
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Pedido al que pertenece este ítem. Si se elimina el pedido, se eliminan
     * todos sus ítems (ON DELETE CASCADE).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /**
     * Código único del producto en MS-Inventario. Se guarda aquí para
     * identificar qué stock descontar cuando MS-Inventario reciba el evento
     * "pedido.creado".
     */
    @Column(nullable = false, length = 80)
    private String sku;

    /**
     * Nombre del producto al momento del pedido (snapshot). Columna: nombre
     */
    @Column(nullable = false, length = 200)
    private String nombre;

    /**
     * Cantidad de unidades solicitadas (debe ser mayor a 0)
     */
    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Precio por unidad al momento del pedido
     */
    @Column(name = "precio_unit", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    // Opción recomendada: calculado en memoria, nunca persistido
    @Transient
    public BigDecimal getSubtotal() {
        if (cantidad != null && precioUnitario != null) {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
        return BigDecimal.ZERO;
    }
}

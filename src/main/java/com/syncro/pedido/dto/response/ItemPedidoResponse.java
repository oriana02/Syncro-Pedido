package com.syncro.pedido.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * Detalle de un producto dentro de la respuesta de un pedido. Muestra el
 * "snapshot" del producto tal como estaba al momento de realizar el pedido:
 * SKU, nombre y precio unitario.
 *
 * Es parte de la lista "items" dentro de PedidoResponse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ItemPedidoResponse {

    /**
     * ID del ítem en la base de datos
     */
    private Long id;

    /**
     * SKU del producto en MS-Inventario
     */
    private String sku;

    /**
     * Nombre del producto al momento del pedido
     */
    private String nombre;

    /**
     * Cantidad solicitada
     */
    private Integer cantidad;

    /**
     * Precio por unidad al momento del pedido
     */
    private BigDecimal precioUnitario;

    /**
     * Subtotal calculado = cantidad * precioUnitario. En Oracle es una columna
     * VIRTUAL (calculada automáticamente).
     */
    private BigDecimal subtotal;

}

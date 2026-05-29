package com.syncro.pedido.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Datos de un producto dentro del pedido. Es un elemento de la lista "items" en
 * CrearPedidoRequest.
 *
 * Ejemplo de JSON esperado: { "sku": "PROD-001", "nombre": "Laptop Dell
 * Inspiron 15", "cantidad": 2, "precioUnitario": 499990.00 }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedidoRequest {

    /**
     * Código único del producto en MS-Inventario. Se usa para identificar qué
     * stock descontar cuando MS-Inventario reciba el evento "pedido.creado".
     */
    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 80, message = "El SKU no puede superar los 80 caracteres")
    private String sku;

    /**
     * Nombre del producto al momento del pedido (snapshot). Se guarda tal como
     * está porque el nombre puede cambiar después en MS-Inventario y el
     * historial debe ser consistente.
     */
    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
    private String nombre;

    /**
     * Cantidad de unidades solicitadas. Mínimo 1.
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    /**
     * Precio por unidad al momento del pedido (snapshot). Se guarda para que el
     * historial sea consistente aunque el precio cambie después en
     * MS-Inventario.
     */
    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;

}

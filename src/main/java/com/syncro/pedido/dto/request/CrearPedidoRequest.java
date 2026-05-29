package com.syncro.pedido.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Datos necesarios para crear un nuevo pedido. El frontend envía este JSON al
 * endpoint POST /pedidos.
 *
 * Ejemplo de JSON esperado: { "empresaId": 1, "usuarioId": 2, "notas":
 * "Entregar en horario de mañana", "referenciaExterna": "ORDER-001",
 * "direccion": { "calle": "Av. Providencia", "numero": "1234", ... }, "items":
 * [ { "sku": "PROD-001", "nombre": "Laptop", "cantidad": 2, ... } ] }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearPedidoRequest {

    /**
     * ID de la empresa que realiza el pedido (FK a tabla empresa)
     */
    @NotNull(message = "empresaId es obligatorio")
    private Long empresaId;

    /**
     * ID del operador que está creando el pedido (FK a tabla usuario)
     */
    @NotNull(message = "usuarioId es obligatorio")
    private Long usuarioId;

    /**
     * Instrucciones o comentarios adicionales (opcional)
     */
    @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
    private String notas;

    /**
     * ID del pedido en el ecommerce externo, como Shopify (opcional)
     */
    @Size(max = 100, message = "La referencia externa no puede superar los 100 caracteres")
    private String referenciaExterna;

    /**
     * Dirección de entrega del pedido. Se usa @Valid para que Spring también
     * valide los campos internos de DireccionRequest (calle, ciudad, región,
     * etc.)
     */
    @NotNull(message = "La dirección de entrega es obligatoria")
    @Valid
    private DireccionRequest direccion;

    /**
     * Lista de productos del pedido. Debe tener al menos 1 ítem. Se usa @Valid
     * para que Spring valide cada ItemPedidoRequest de la lista.
     */
    @NotEmpty(message = "El pedido debe tener al menos un producto")
    @Valid
    private List<ItemPedidoRequest> items;

}

package com.syncro.pedido.event;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoCreadoEvent {

    private Long pedidoId;
    private Long empresaId;

    // Los items son lo más importante para inventario: SKU y cantidad a descontar
    private List<ItemEvento> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemEvento {

        private String sku;
        private Integer cantidad;
        private BigDecimal precioUnitario;
    }

}

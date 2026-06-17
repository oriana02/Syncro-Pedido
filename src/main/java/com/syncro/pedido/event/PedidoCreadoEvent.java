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
    private String destinatarioNombre;
    private String destinatarioEmail;
    private String destinatarioTel;
    private String direccionCalle;
    private String direccionNumero;
    private String direccionDepto;
    private String direccionCiudad;
    private String direccionRegion;
    private String direccionPais;
    private String codigoPostal;
    private List<ItemEvento> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemEvento {
        private String sku;
        private String nombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;
    }
}
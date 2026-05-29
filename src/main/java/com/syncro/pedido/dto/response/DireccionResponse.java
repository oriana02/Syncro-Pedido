package com.syncro.pedido.dto.response;

import lombok.*;

/**
 * Dirección de entrega dentro de la respuesta de un pedido. Refleja los datos
 * de la entidad DireccionEntrega pero como DTO de solo lectura para no exponer
 * la entidad directamente.
 *
 * Es parte del objeto "direccion" dentro de PedidoResponse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class DireccionResponse {

    /**
     * ID de la dirección en la base de datos
     */
    private Long id;

    /**
     * Nombre de la calle
     */
    private String calle;

    /**
     * Número del domicilio
     */
    private String numero;

    /**
     * Departamento o piso (null si no aplica)
     */
    private String depto;

    /**
     * Ciudad de destino
     */
    private String ciudad;

    /**
     * Región de destino
     */
    private String region;

    /**
     * País de destino
     */
    private String pais;

    /**
     * Código postal (null si no fue ingresado)
     */
    private String codigoPostal;

    /**
     * Indicaciones adicionales para el repartidor
     */
    private String referencia;

}

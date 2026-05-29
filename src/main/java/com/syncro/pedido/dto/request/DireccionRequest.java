package com.syncro.pedido.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos de la dirección de entrega dentro de un pedido. Es un sub-objeto dentro
 * de CrearPedidoRequest.
 *
 * Ejemplo de JSON esperado: { "calle": "Av. Providencia", "numero": "1234",
 * "depto": "Depto 502", "ciudad": "Santiago", "region": "Región Metropolitana",
 * "pais": "Chile", "referencia": "Casa con reja verde" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionRequest {

    /**
     * Nombre de la calle
     */
    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 200, message = "La calle no puede superar los 200 caracteres")
    private String calle;

    /**
     * Número del domicilio (ej: "1234")
     */
    @Size(max = 20, message = "El número no puede superar los 20 caracteres")
    private String numero;

    /**
     * Departamento o piso, opcional (ej: "Depto 502")
     */
    @Size(max = 30, message = "El depto no puede superar los 30 caracteres")
    private String depto;

    /**
     * Ciudad de destino
     */
    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
    private String ciudad;

    /**
     * Región de destino
     */
    @NotBlank(message = "La región es obligatoria")
    @Size(max = 100, message = "La región no puede superar los 100 caracteres")
    private String region;

    /**
     * País de destino. Si no se envía, el service asigna "Chile" por defecto
     */
    @Size(max = 60, message = "El país no puede superar los 60 caracteres")
    private String pais;

    /**
     * Código postal, opcional
     */
    @Size(max = 20, message = "El código postal no puede superar los 20 caracteres")
    private String codigoPostal;

    /**
     * Indicaciones adicionales para el repartidor (ej: "Tocar timbre 2 veces")
     */
    @Size(max = 300, message = "La referencia no puede superar los 300 caracteres")
    private String referencia;

}

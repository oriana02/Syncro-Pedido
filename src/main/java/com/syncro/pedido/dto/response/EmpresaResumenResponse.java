package com.syncro.pedido.dto.response;

import lombok.*;

/**
 * Información resumida de la empresa para incluir dentro de la respuesta de un
 * pedido (PedidoResponse).
 *
 * Solo expone los datos necesarios para identificar a la empresa. Evita exponer
 * todos los campos de la entidad Empresa (email, teléfono, etc.) cuando solo se
 * necesita mostrar el nombre en un pedido.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class EmpresaResumenResponse {

    /**
     * ID de la empresa en la base de datos
     */
    private Long id;

    /**
     * Nombre comercial de la empresa
     */
    private String nombre;

    /**
     * RUT de la empresa
     */
    private String rut;

}

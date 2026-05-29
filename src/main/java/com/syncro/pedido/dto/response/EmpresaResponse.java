package com.syncro.pedido.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaResponse {

    private Long id;
    private String nombre;
    private String rut;
    private String email;
    private String telefono;
    private Boolean activo;
    private LocalDateTime fechaCreacion;

}

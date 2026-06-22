package com.syncro.pedido.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Representa a las PYMEs que usan la plataforma Syncro. Una empresa puede tener
 * múltiples usuarios y pedidos. Tabla: empresa
 */
@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Clave primaria generada por la secuencia de MYSQL
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre comercial de la PYME
     */
    @Column(nullable = false, length = 150)
    private String nombre;

    /**
     * RUT de la empresa, debe ser único en el sistema
     */
    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    /**
     * Email de contacto principal, también único
     */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Teléfono de contacto (opcional)
     */
    @Column(length = 20)
    private String telefono;

    /**
     * Indica si la empresa está activa en el sistema. true = activa, false =
     * desactivada
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /**
     * Fecha en que se registró la empresa
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

}

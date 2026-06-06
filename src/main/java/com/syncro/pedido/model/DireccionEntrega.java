package com.syncro.pedido.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Contiene los datos de destino de un pedido. Se guarda como tabla separada
 * para reutilizar direcciones y mantener un historial limpio de entregas.
 * Tabla: direccion_entrega
 */
@Entity
@Table(name = "direccion_entrega")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionEntrega {

    /**
     * Clave primaria generada por AUTO_INCREMENT de MYSQL
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de la calle
     */
    @Column(nullable = false, length = 200)
    private String calle;

    /**
     * Número del domicilio (ej: 1234)
     */
    @Column(length = 20)
    private String numero;

    /**
     * Departamento o piso (opcional, ej: Depto 502)
     */
    @Column(length = 30)
    private String depto;

    /**
     * Ciudad de destino
     */
    @Column(nullable = false, length = 100)
    private String ciudad;

    /**
     * Región de destino
     */
    @Column(nullable = false, length = 100)
    private String region;

    /**
     * País de destino, por defecto Chile
     */
    @Column(nullable = false, length = 60)
    @Builder.Default
    private String pais = "Chile";

    /**
     * Código postal (opcional)
     */
    @Column(length = 20)
    private String codigoPostal;

    /**
     * Indicaciones adicionales para el repartidor (ej: "Casa con reja verde")
     */
    @Column(length = 300)
    private String referencia;
}

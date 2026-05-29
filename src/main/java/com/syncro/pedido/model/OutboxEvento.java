package com.syncro.pedido.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "outbox_evento")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long pedidoId;
    private String tipoEvento;
    @Column(columnDefinition = "JSON")
    private String payload;
    private boolean enviado;
    private int intentos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnviado;
    private String errorMensaje;
}

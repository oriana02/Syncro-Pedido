package com.syncro.pedido.event;

import com.syncro.pedido.model.OutboxEvento;
import com.syncro.pedido.repository.OutboxEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock OutboxEventoRepository outboxRepository;
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks OutboxScheduler scheduler;

    private OutboxEvento evento;

    @BeforeEach
    void setUp() {
        evento = new OutboxEvento();
        evento.setId(1L);
        evento.setPayload("{\"pedidoId\":1}");
        evento.setEnviado(false);
        evento.setIntentos(0);
    }

    @Test
    @DisplayName("procesarPendientes - envia mensaje y marca como enviado")
    void procesarPendientes_enviaOk() {
        when(outboxRepository.findByEnviadoFalseAndIntentosLessThan(5))
            .thenReturn(List.of(evento));

        scheduler.procesarPendientes();

        assertThat(evento.isEnviado()).isTrue();
        assertThat(evento.getFechaEnviado()).isNotNull();
        verify(outboxRepository).save(evento);
    }

    @Test
    @DisplayName("procesarPendientes - sin pendientes no hace nada")
    void procesarPendientes_sinPendientes() {
        when(outboxRepository.findByEnviadoFalseAndIntentosLessThan(5))
            .thenReturn(List.of());

        scheduler.procesarPendientes();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("procesarPendientes - si RabbitMQ falla incrementa intentos")
    void procesarPendientes_falloRabbit() {
        when(outboxRepository.findByEnviadoFalseAndIntentosLessThan(5))
            .thenReturn(List.of(evento));
        doThrow(new AmqpException("RabbitMQ caido"))
            .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        scheduler.procesarPendientes();

        assertThat(evento.isEnviado()).isFalse();
        assertThat(evento.getIntentos()).isEqualTo(1);
        assertThat(evento.getErrorMensaje()).contains("RabbitMQ caido");
        verify(outboxRepository).save(evento);
    }

    @Test
    @DisplayName("enviarConCircuitBreaker - fallback loguea error sin lanzar excepcion")
    void fallbackEnvio_noLanzaExcepcion() {
        Message message = new Message(new byte[0]);
        scheduler.fallbackEnvio(message, new RuntimeException("Conexion rechazada"));
    }
}
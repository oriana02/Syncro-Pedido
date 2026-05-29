package com.syncro.pedido.event;

import com.syncro.pedido.model.OutboxEvento;
import com.syncro.pedido.repository.OutboxEventoRepository;
import com.syncro.pedido.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxEventoRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_INTENTOS = 5;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void procesarPendientes() {
        List<OutboxEvento> pendientes =
                outboxRepository.findByEnviadoFalseAndIntentosLessThan(MAX_INTENTOS);

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("Outbox: procesando {} eventos pendientes", pendientes.size());

        for (OutboxEvento evento : pendientes) {
            try {
                MessageProperties props = new MessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);

                Message message = new Message(
                        evento.getPayload().getBytes(StandardCharsets.UTF_8),
                        props
                );

                rabbitTemplate.send(RabbitMQConfig.EXCHANGE, "", message);

                evento.setEnviado(true);
                evento.setFechaEnviado(LocalDateTime.now());
                log.info("Outbox ID={} enviado OK (intento {})",
                        evento.getId(), evento.getIntentos() + 1);

            } catch (Exception e) {
                evento.setIntentos(evento.getIntentos() + 1);
                evento.setErrorMensaje(e.getMessage());
                log.warn("Outbox ID={} fallo intento {}/{}",
                        evento.getId(), evento.getIntentos(), MAX_INTENTOS);
            }
            outboxRepository.save(evento);
        }
    }
}

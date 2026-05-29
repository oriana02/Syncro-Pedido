package com.syncro.pedido.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "pedidos.exchange";
    public static final String COLA_INVENTARIO = "inventario.sincronizar";
    public static final String COLA_ENVIOS = "envio.generar";

    // El exchange tipo Fanout distribuye el mensaje a TODAS las colas enlazadas
    @Bean
    public FanoutExchange exchange() {
        return new FanoutExchange(EXCHANGE);
    }

    // Cola que escucha MS-Inventario
    @Bean
    public Queue colaInventario() {
        return new Queue(COLA_INVENTARIO, true); // true = durable, sobrevive reinicios
    }

    // Cola que escucha MS-Envios
    @Bean
    public Queue colaEnvios() {
        return new Queue(COLA_ENVIOS, true);
    }

    // Enlazar cada cola al exchange
    @Bean
    public Binding bindingInventario(Queue colaInventario, FanoutExchange exchange) {
        return BindingBuilder.bind(colaInventario).to(exchange);
    }

    @Bean
    public Binding bindingEnvios(Queue colaEnvios, FanoutExchange exchange) {
        return BindingBuilder.bind(colaEnvios).to(exchange);
    }

    // Convertidor JSON — serializa el objeto Java a JSON automáticamente
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());

        //si rabbit rechaza el mensaje, scheduler lo reintente
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                
                System.err.println("Mensaje no confirmado: " + cause);
            }
        });
        return template;
    }

}

package com.syncro.pedido.exception;

public class TransaccionEstadoInvalidaException extends RuntimeException {

    public TransaccionEstadoInvalidaException(String actual, String nuevo) {
        super("Transición de estado inválida: " + actual + " a " + nuevo);

    } 
    
}

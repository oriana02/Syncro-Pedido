package com.syncro.pedido.exception;

public class PedidoNotFoundException extends RuntimeException {

    public PedidoNotFoundException(Long id) {
        super("No se encontró el pedido con ID: " + id);
    }

}

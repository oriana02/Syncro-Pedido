package com.syncro.pedido.model;

/**
 * Roles disponibles para los usuarios del sistema. Deben coincidir con el CHECK
 * constraint en la tabla usuario del SQL.
 */
public enum Rol {

    /**
     * Acceso total: puede crear empresas, usuarios y ver todos los pedidos
     */
    ADMIN,
    /**
     * Acceso estándar: puede crear y gestionar pedidos
     */
    OPERADOR,
    /**
     * Acceso restringido: solo puede actualizar estados de pedidos en bodega
     */
    BODEGUERO
}

package com.syncro.pedido.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncro.pedido.model.EstadoPedido;
import com.syncro.pedido.model.HistorialEstado;

/**
 * Repositorio de acceso a datos para la entidad HistorialEstado.
 *
 * Guarda cada cambio de estado que sufre un pedido. Permite trazabilidad
 * completa: se puede saber exactamente qué pasó con un pedido, cuándo y quién o modificó (RF-1.2).
 *
 * Nota: en la mayoría de casos el historial se guarda automáticamente a través
 * de la relación CascadeType.ALL en Pedido → HistorialEstado. Este repositorio
 * se usa cuando se necesita consultar el historial de forma independiente, sin
 * cargar todo el pedido.
 */
@Repository
public interface HistorialRepository extends JpaRepository<HistorialEstado, Long> {

    /**
     * Obtiene todo el historial de estados de un pedido, ordenado del cambio
     * más antiguo al más reciente. Permite ver la línea de tiempo completa del
     * pedido.
     *
     * Spring traduce este nombre a: SELECT * FROM historial_estado_pedido WHERE
     * pedido_id = ? ORDER BY fecha_cambio ASC
     */
    List<HistorialEstado> findByPedido_IdOrderByFechaCambioAsc(Long pedidoId);

    /**
     * Busca todos los cambios a un estado específico dentro de un rango de
     * fechas. Útil para reportes: ej. cuántos pedidos se confirmaron en un día.
     *
     * Spring traduce este nombre a: SELECT * FROM historial_estado_pedido WHERE
     * estado_nuevo = ? AND fecha_cambio BETWEEN ? AND ?
     */
    List<HistorialEstado> findByEstadoNuevoAndFechaCambioBetween(
            EstadoPedido estadoNuevo,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    /**
     * Busca los cambios realizados por un usuario específico. Permite auditar
     * las acciones de un operador en particular.
     *
     * Spring traduce este nombre a: SELECT * FROM historial_estado_pedido WHERE
     * actor_id = ?
     */
    List<HistorialEstado> findByActorId(Long actorId);

}

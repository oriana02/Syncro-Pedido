package com.syncro.pedido.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.syncro.pedido.model.EstadoPedido;
import com.syncro.pedido.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Pedido.
 *
 * Extiende JpaRepository, lo que proporciona automáticamente los métodos
 * básicos sin escribir código: save(), findById(), findAll(), delete(), etc.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Busca todos los pedidos de una empresa ordenados del más reciente al más
     * antiguo. Usado en: GET /pedidos/historial/{empresaId}
     */
    List<Pedido> findByEmpresa_IdOrderByFechaCreacionDesc(Long empresaId);

    /**
     * Busca pedidos de una empresa filtrados por estado. Usado en: GET
     * /pedidos/historial/{empresaId}?estado=CONFIRMADO
     */
    List<Pedido> findByEmpresa_IdAndEstado(Long empresaId, EstadoPedido estado);

    /**
     * Busca pedidos de una empresa dentro de un rango de fechas. Usado en: GET
     * /pedidos/historial/{empresaId}?desde=...&hasta=...
     */
    List<Pedido> findByEmpresa_IdAndFechaCreacionBetween(
            Long empresaId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    /**
     * Busca todos los pedidos que están en un estado específico. 
     */
    List<Pedido> findByEstado(EstadoPedido estado);

    /**
     * Busca pedidos que aún no han publicado el evento en RabbitMQ. Sirve como
     * mecanismo de recuperación: si el sistema falla antes de publicar el
     * evento, este método detecta los pedidos confirmados pendientes.
     *
     * Usa @Query con JPQL porque combina dos condiciones sobre campos que
     * harían el nombre del método demasiado largo.
     */
    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado AND p.eventoPublicado = false")
    List<Pedido> findPedidosConfirmadosSinEvento(@Param("estado") EstadoPedido estado);

    /**
     * Busca pedidos por la referencia del ecommerce externo (ej: Shopify,
     * WooCommerce). Permite relacionar un pedido interno de Syncro con el
     * pedido original de la plataforma externa del cliente.
     *
     * Spring traduce este nombre a: SELECT * FROM pedido WHERE
     * referencia_externa = ?
     */
    List<Pedido> findByReferenciaExterna(String referenciaExterna);

    /**
     * Verifica si un pedido pertenece a una empresa específica. Se usa para
     * validar que un operador solo pueda ver/modificar pedidos de su propia
     * empresa (control de acceso por empresa).
     *
     * Spring traduce este nombre a: SELECT COUNT(*) > 0 FROM pedido WHERE id =
     * ? AND empresa_id = ?
     */
    boolean existsByIdAndEmpresa_Id(Long pedidoId, Long empresaId);
}

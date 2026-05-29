package com.syncro.pedido.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.syncro.pedido.model.DireccionEntrega;

/**
 * Repositorio de acceso a datos para la entidad DireccionEntrega.
 *
 * Las direcciones se guardan como registros independientes para: - Mantener un
 * historial consistente: aunque el cliente cambie su dirección después, el
 * pedido siempre muestra la dirección original. - Reutilizar direcciones
 * frecuentes sin duplicar datos.
 *
 * En la mayoría de los casos, la dirección se crea junto con el pedido usando
 * CascadeType.PERSIST en la relación Pedido → DireccionEntrega.
 */
@Repository
public interface DireccionEntregaRepository extends JpaRepository<DireccionEntrega, Long> {

    /**
     * Busca todas las direcciones de una ciudad específica. Puede usarse para
     * reportes logísticos o análisis de zonas de entrega.
     *
     * Spring traduce este nombre a: SELECT * FROM direccion_entrega WHERE
     * ciudad = ?
     */
    List<DireccionEntrega> findByCiudad(String ciudad);

    /**
     * Busca todas las direcciones de una región específica. Útil para calcular
     * costos de envío por zona geográfica.
     *
     * Spring traduce este nombre a: SELECT * FROM direccion_entrega WHERE
     * region = ?
     */
    List<DireccionEntrega> findByRegion(String region);

}

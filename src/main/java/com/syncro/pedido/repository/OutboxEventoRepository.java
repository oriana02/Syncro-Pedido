package com.syncro.pedido.repository;

import org.springframework.stereotype.Repository;
import com.syncro.pedido.model.OutboxEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


@Repository
public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, Long> {
    List<OutboxEvento> findByEnviadoFalseAndIntentosLessThan(int maxIntentos);
}
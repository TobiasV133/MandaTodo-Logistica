package com.mandatodo.logistica.revision;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionRepository extends JpaRepository<Revision, Long> {

    List<Revision> findByPedidoIdOrderByIdAsc(Long pedidoId);

    List<Revision> findByPedidoIdAndEstadoResolucionOrderByIdAsc(
        Long pedidoId,
        EstadoResolucion estadoResolucion
    );

    List<Revision> findByPedidoIdInOrderByIdAsc(Collection<Long> pedidoIds);
}

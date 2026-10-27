package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.EstadoResolucion;
import com.mandatodo.backend.model.MotivoRetencion;
import com.mandatodo.backend.model.Revision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio JPA para la bandeja de seguridad de revisiones.
 */
public interface RevisionRepository extends JpaRepository<Revision, Integer> {

    List<Revision> findByPedidoIdPedido(Integer pedidoId);

    List<Revision> findByEstadoResolucion(EstadoResolucion estadoResolucion);

    long countByEstadoResolucion(EstadoResolucion estadoResolucion);

    @Query("""
            select r.pedido.idPedido as idPedido, r.motivoRetencion as motivo
            from Revision r
            where r.pedido.idPedido in :ids
              and r.estadoResolucion = com.mandatodo.backend.model.EstadoResolucion.PENDIENTE
            order by r.idRevision desc
            """)
    List<MotivoRevisionProjection> buscarMotivosVigentes(@Param("ids") Collection<Integer> ids);

    interface MotivoRevisionProjection {
        Integer getIdPedido();

        MotivoRetencion getMotivo();
    }
}

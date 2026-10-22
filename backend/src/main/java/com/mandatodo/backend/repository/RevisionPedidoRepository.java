package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.RevisionPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RevisionPedidoRepository extends JpaRepository<RevisionPedido, Integer> {

    @Query("""
            select r.pedido.idPedido as idPedido, r.motivoRetencion as motivo
            from RevisionPedido r
            where r.pedido.idPedido in :ids
              and r.resuelto = false
            order by r.fechaRevision desc
            """)
    List<MotivoRevisionProjection> buscarMotivosVigentes(@Param("ids") Collection<Integer> ids);

    long countByResueltoFalse();

    interface MotivoRevisionProjection {
        Integer getIdPedido();

        String getMotivo();
    }
}

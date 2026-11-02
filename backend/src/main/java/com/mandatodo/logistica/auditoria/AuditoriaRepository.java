package com.mandatodo.logistica.auditoria;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio deliberadamente acotado: expone solo operaciones de alta y
 * consulta para reforzar el carácter append-only de la auditoría.
 */
public interface AuditoriaRepository extends Repository<Auditoria, Long> {

    Auditoria save(Auditoria auditoria);

    List<Auditoria> findAllByOrderByFechaHoraDesc();

    /**
     * Filtros opcionales del contrato GET /api/auditoria. `hasta` se interpreta
     * como límite exclusivo para no depender de la última hora del día.
     */
    @Query("""
        select a from Auditoria a
        where (:desde is null or a.fechaHora >= :desde)
          and (:hasta is null or a.fechaHora < :hasta)
          and (:idOperario is null or a.operario.id = :idOperario)
          and (:tipoEvento is null or a.tipoEvento = :tipoEvento)
        order by a.fechaHora desc
        """)
    List<Auditoria> buscar(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("idOperario") Long idOperario,
        @Param("tipoEvento") TipoEvento tipoEvento
    );

    long countByPedido_IdAndActividadStartingWith(Long idPedido, String prefijo);

    long countByPedido_IdAndActividadContaining(Long idPedido, String texto);
}

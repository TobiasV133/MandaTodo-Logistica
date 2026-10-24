package com.mandatodo.logistica.auditoria;

import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Repositorio deliberadamente acotado: expone solo operaciones de alta y
 * consulta para reforzar el carácter append-only de la auditoría.
 */
public interface AuditoriaRepository extends Repository<Auditoria, Long> {

    Auditoria save(Auditoria auditoria);

    List<Auditoria> findAllByOrderByFechaHoraDesc();

    long countByPedido_IdAndActividadStartingWith(Long idPedido, String prefijo);
}

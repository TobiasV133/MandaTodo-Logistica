package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.Auditoria;
import com.mandatodo.backend.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para la auditoría global e inalterable.
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByTipoEvento(TipoEvento tipoEvento);

    List<Auditoria> findByOperarioIdOperario(Integer operarioId);

    List<Auditoria> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Auditoria> findAllByOrderByFechaHoraDesc();

    List<Auditoria> findByTipoEventoOrderByFechaHoraDesc(TipoEvento tipoEvento);

    List<Auditoria> findByOperarioIdOperarioOrderByFechaHoraDesc(Integer operarioId);

    List<Auditoria> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);
}

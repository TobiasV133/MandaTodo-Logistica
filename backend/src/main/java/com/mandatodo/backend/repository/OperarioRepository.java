package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.EstadoOperario;
import com.mandatodo.backend.model.Operario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para los operarios del sistema.
 */
public interface OperarioRepository extends JpaRepository<Operario, Integer> {

    Optional<Operario> findByEmail(String email);

    Optional<Operario> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdOperarioNot(String email, Integer idOperario);

    List<Operario> findByEstado(EstadoOperario estado);

    List<Operario> findAllByOrderByNombreAsc();

    List<Operario> findByEstadoOrderByNombreAsc(EstadoOperario estado);
}

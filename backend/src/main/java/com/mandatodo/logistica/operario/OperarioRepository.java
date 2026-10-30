package com.mandatodo.logistica.operario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperarioRepository extends JpaRepository<Operario, Long> {

    Optional<Operario> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<Operario> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Operario> findAllByOrderByNombreAsc();

    List<Operario> findByEstadoOrderByNombreAsc(EstadoOperario estado);
}

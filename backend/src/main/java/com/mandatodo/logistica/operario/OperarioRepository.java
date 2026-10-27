package com.mandatodo.logistica.operario;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperarioRepository extends JpaRepository<Operario, Long> {

    Optional<Operario> findByEmailIgnoreCase(String email);
}

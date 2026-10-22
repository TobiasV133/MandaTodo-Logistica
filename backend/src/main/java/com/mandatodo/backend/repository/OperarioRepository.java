package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.Operario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperarioRepository extends JpaRepository<Operario, Integer> {

    Optional<Operario> findByUsername(String username);

    boolean existsByUsername(String username);
}

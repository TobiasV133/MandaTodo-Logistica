package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    Optional<Cliente> findByEmailIgnoreCase(String email);

    List<Cliente> findAllByOrderByFechaCreacionDesc();
}

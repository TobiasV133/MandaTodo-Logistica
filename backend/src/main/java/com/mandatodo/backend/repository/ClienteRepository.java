package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la libreta digital de clientes.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    Optional<Cliente> findByEmailIgnoreCase(String email);

    List<Cliente> findAllByOrderByFechaCreacionDesc();

    List<Cliente> findByEmail(String email);

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdClienteNot(String email, Integer idCliente);

    List<Cliente> findAllByOrderByNombreRazonSocialAsc();
}

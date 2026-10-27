package com.mandatodo.logistica.cliente;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    /**
     * Los seeds guardan los CUIT con guiones (30-71234567-8) y el frontend
     * envía solo dígitos. La comparación normalizada permite encontrar ambos.
     */
    @Query("""
        select c from Cliente c
        where replace(replace(replace(c.numeroDocumento, '-', ''), '.', ''), ' ', '') = :documento
        """)
    Optional<Cliente> findByDocumentoNormalizado(@Param("documento") String documento);

    List<Cliente> findAllByOrderByNombreRazonSocialAsc();
}

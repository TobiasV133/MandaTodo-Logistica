package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.EstadoPedido;
import com.mandatodo.backend.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    @EntityGraph(attributePaths = "cliente")
    List<Pedido> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = "cliente")
    List<Pedido> findByEstadoOrderByFechaCreacionDesc(EstadoPedido estado);

    long countByEstado(EstadoPedido estado);

    boolean existsByCodigoPedido(String codigoPedido);

    List<Pedido> findByEstado(EstadoPedido estado);

    List<Pedido> findByClienteIdCliente(Integer idCliente);

    /**
     * Un pedido se considera duplicado comercial cuando coincide el cliente, la dirección,
     * la descripción del paquete y fue creado durante el día en curso.
     */
    @Query("""
            select (count(p) > 0)
            from Pedido p
            where p.cliente.idCliente = :idCliente
              and lower(trim(p.calle)) = lower(trim(:calle))
              and coalesce(p.altura, -1) = coalesce(:altura, -1)
              and lower(coalesce(p.pisoDepto, '')) = lower(coalesce(:pisoDepto, ''))
              and lower(trim(p.localidad)) = lower(trim(:localidad))
              and lower(trim(p.descripcionPaquete)) = lower(trim(:descripcionPaquete))
              and p.fechaCreacion >= :inicioDia
              and p.fechaCreacion < :finDia
              and p.estado <> :estadoExcluido
            """)
    boolean existsDuplicadoComercialDelDia(
            @Param("idCliente") Integer idCliente,
            @Param("calle") String calle,
            @Param("altura") Integer altura,
            @Param("pisoDepto") String pisoDepto,
            @Param("localidad") String localidad,
            @Param("descripcionPaquete") String descripcionPaquete,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia,
            @Param("estadoExcluido") EstadoPedido estadoExcluido
    );
}
